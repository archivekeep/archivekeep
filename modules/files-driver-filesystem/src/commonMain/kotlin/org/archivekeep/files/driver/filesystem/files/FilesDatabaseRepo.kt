package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.archivekeep.files.api.exceptions.ChecksumMismatch
import org.archivekeep.files.api.exceptions.DestinationExists
import org.archivekeep.files.api.exceptions.NotRegularFilePath
import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.utils.coroutines.InProgressHandler
import org.archivekeep.utils.coroutines.flowScopedToThisJob
import org.archivekeep.utils.flows.logLoadableResourceLoad
import org.archivekeep.utils.io.AutomaticFileCleanup
import org.archivekeep.utils.io.createTmpFileForWrite
import org.archivekeep.utils.io.moveTmpToDestination
import org.archivekeep.utils.io.watchRecursively
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.produceLoadable
import org.archivekeep.utils.loading.produceLoadableStateIn
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.safeFileReadWrite
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util.*
import java.util.Collections.singletonList
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.moveTo
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val ignorePatternsFileName = ".archivekeepignore"

/**
 * Shared repository implementation for database-backed file repositories.
 * Uses [DatabaseIndexStore] to abstract the storage layer (SQLite, ObjectBox, etc.).
 */
abstract class FilesDatabaseRepo(
    override val root: Path,
    parentJob: Job? = null,
    internal val archiveRoot: Path = root.resolve(".archive"),
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FilesystemWorkingRepository {
    abstract val indexStore: DatabaseIndexStore

    private fun initIndexFlow(): StateFlow<Loadable<RepoIndex>> =
        indexStore.files
            .map { entries ->
                RepoIndex(
                    files =
                        entries.map {
                            RepoIndex.File(
                                path = it.path,
                                size = it.size,
                                checksumSha256 = it.checksumSha256,
                            )
                        },
                )
            }.mapToLoadable()
            .stateIn(scope)

    private fun initLocalIndex(): Flow<Loadable<StatusOperation.Result>> {
        @OptIn(FlowPreview::class)
        val calcCause =
            root
                .watchRecursively(ioDispatcher, ioDispatcher)
                .map { "update" }
                .debounce(100.milliseconds)
                .shareIn(scope, SharingStarted.WhileSubscribed(), 0)
                .onStart { emit("start on index change") }

        @OptIn(ExperimentalCoroutinesApi::class)
        return indexStore
            .files
            .mapToLoadable()
            .flatMapLoadableFlow { index ->
                val indexFiles = index.map { it.path }.toSet()

                inProgressHandler
                    .jobActiveOnIdleDelayedStart
                    .flowScopedToThisJob {
                        calcCause
                            .produceLoadable(
                                ioDispatcher,
                                "Local status: $root",
                            ) {
                                val allFiles =
                                    findAllFiles(listOf("*"))
                                        .map {
                                            it.invariantSeparatorsPathString
                                        }.toSortedSet()

                                currentCoroutineContext().ensureActive()

                                StatusOperation.Result(
                                    newFiles = allFiles.filter { !indexFiles.contains(it) },
                                    indexedFiles = allFiles.filter { indexFiles.contains(it) },
                                    modifiedIndexedFiles =
                                        index
                                            .filter {
                                                val path = root.resolve(it.path)

                                                if (!path.exists()) {
                                                    return@filter false
                                                }

                                                val timestampChanged =
                                                    areTimestampsDifferent(
                                                        path.getLastModifiedTime().toMillis(),
                                                        it.lastModified.toInstant().toEpochMilli(),
                                                    )
                                                val sizeChanged = path.fileSize() != it.size

                                                timestampChanged || sizeChanged
                                            }.map { it.path },
                                    missingFiles = indexFiles.filter { !allFiles.contains(it) },
                                )
                            }
                    }
            }.logLoadableResourceLoad("Local status: $root")
            .flowOn(ioDispatcher)
            .stateIn(scope)
    }

    private val job = SupervisorJob(parentJob)
    private val scope = CoroutineScope(job + CoroutineName("FilesDatabaseRepo: $root") + stateDispatcher)

    private val metadataPath = root.resolve(".archive").resolve("metadata.json")

    private val inProgressHandler =
        InProgressHandler<Path>(scope, transform = { it.absolutePathString() })

    private val autoRepairJob: Job by lazy {
        scope.launch(ioDispatcher) {
            try {
                autoRemoveDeadIncomingFiles()
                autoRemovePendingMoves()

                delay(durationToDeath + 1.seconds)
                autoRemoveDeadIncomingFiles()
                autoRemovePendingMoves()
            } catch (e: Throwable) {
                println("Error during auto-repair: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Call this after construction to start the auto-repair job.
     * Subclasses should call this from their constructor after indexStore is initialized.
     */
    protected fun startAutoRepair() {
        autoRepairJob
    }

    suspend fun autoRemoveDeadIncomingFiles() {
        indexStore.getDeadIncomingFiles().forEach { file ->
            println("Repairing dead incoming file: ${file.tmpWritePath} -> ${file.path}")

            val tmpFile = root.resolve(safeSubPath(file.tmpWritePath))

            if (tmpFile.exists()) {
                tmpFile.deleteIfExists()
            }

            indexStore.remove(file.path)
        }
    }

    suspend fun autoRemovePendingMoves() {
        indexStore.getDeadPendingMoves().forEach { pendingMove ->
            // Pending moves are automatically cleaned up
        }
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun deinitialize() {
        withContext(ioDispatcher) {
            archiveRoot.deleteRecursively()
        }
    }

    override fun getFileSize(filename: String): Long? {
        val path = root.resolve(safeSubPath(filename))

        return if (path.exists()) path.fileSize() else null
    }

    override suspend fun findAllFiles(globs: List<String>): List<Path> {
        val matchers =
            globs
                .map {
                    val absolutePath = root.resolve(it).normalize().toString()
                    FileSystems.getDefault().getPathMatcher("glob:$absolutePath")
                }

        val ignorePatterns = loadIgnorePatterns()

        return Files
            .walk(root)
            .filter { path: Path? -> if (path != null) matchers.any { it.matches(path) } else false }
            .flatMap {
                if (it.isRegularFile()) {
                    singletonList(it).stream()
                } else {
                    Files.walk(it).filter { it.isRegularFile() }
                }
            }.asSequence()
            .map { it.relativeTo(root) }
            .filter { path ->
                val parts =
                    path
                        .invariantSeparatorsPathString
                        .split("/")

                parts[0] != ".archive" &&
                    parts[0] != ignorePatternsFileName &&
                    parts.none { part ->
                        ignorePatterns.any {
                            it.matches(
                                Path(part),
                            )
                        }
                    }
            }.toList()
    }

    override suspend fun indexedFilenames(): List<String> =
        indexStore.files
            .flowOn(ioDispatcher)
            .first()
            .map { it.path }

    override suspend fun verifyFileExists(path: String): Boolean {
        val fullPath = root.resolve(safeSubPath(path))

        return fullPath.exists() && fullPath.isRegularFile()
    }

    override suspend fun fileChecksum(path: String): String = indexStore.file(path)!!.checksumSha256

    override suspend fun computeFileChecksum(path: Path): String {
        val fullPath = root.resolve(safeSubPath(path))

        return computeChecksum(fullPath)
    }

    override suspend fun add(
        path: String,
        reindex: Boolean,
    ) {
        val fullPath = root.resolve(safeSubPath(path))
        val size = fullPath.fileSize()
        val lastModified = Date(fullPath.getLastModifiedTime().toMillis())
        val checksum = computeChecksum(fullPath)

        indexStore.storeFile(path, size, lastModified, checksum, reindex = reindex)
    }

    override suspend fun delete(filename: String) {
        val fullPath = root.resolve(safeSubPath(filename))

        if (!fullPath.isRegularFile()) {
            throw NotRegularFilePath(fullPath.toString())
        }

        fullPath.deleteExisting()
        indexStore.remove(filename)
    }

    override suspend fun remove(path: String) {
        indexStore.remove(path)
    }

    override suspend fun move(
        from: String,
        to: String,
    ) {
        withContext(Dispatchers.IO) {
            val dstPath = root.resolve(safeSubPath(to))
            if (dstPath.exists()) {
                throw DestinationExists(to)
            }

            indexStore.beginMove(from, to, Date()).run {
                val lastModified =
                    try {
                        dstPath.createParentDirectories()
                        root.resolve(from).moveTo(dstPath)
                        Date(dstPath.getLastModifiedTime().toMillis())
                    } catch (e: Throwable) {
                        withContext(NonCancellable) {
                            failed()
                        }
                        throw e
                    }
                completed(lastModified)
            }
        }
    }

    override suspend fun <T> open(
        filename: String,
        block: suspend (ArchiveFileInfo, InputStream) -> T,
    ): T {
        val checksum = fileChecksum(filename)
        val realPath = root.resolve(safeSubPath(filename))

        return realPath.inputStream().use { inputStream ->
            block(
                ArchiveFileInfo(
                    length = realPath.fileSize(),
                    checksumSha256 = checksum,
                ),
                inputStream,
            )
        }
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
    ) {
        val dstPath = root.resolve(safeSubPath(filename))

        if (dstPath.exists()) {
            throw DestinationExists(filename)
        }

        withContext(Dispatchers.IO) {
            dstPath.createParentDirectories()

            val (dstTmpFilePath, fc) = createTmpFileForWrite(dstPath, FileChannel::open)

            val cleanup = AutomaticFileCleanup()

            try {
                inProgressHandler.onStart(dstPath)
                indexStore.storeIncomingFile(filename, dstTmpFilePath.relativeTo(root).pathString, info.length, info.checksumSha256, Date())

                val lastModified =
                    try {
                        coroutineScope {
                            val lastAliveKeeper =
                                launch {
                                    indexStore.updateIncomingFileLastAlive(filename, Date())
                                    delay(aliveUpdateFrequency)
                                }

                            try {
                                cleanup.files.add(dstTmpFilePath)

                                fc.use { output ->
                                    val buffer = ByteArray(2 * 1024 * 1024)
                                    var read: Int = stream.read(buffer)
                                    var total: Long = 0
                                    var lastSync: Long = 0

                                    while (read != -1) {
                                        currentCoroutineContext().ensureActive()

                                        output.write(ByteBuffer.wrap(buffer, 0, read))

                                        total += read

                                        read = stream.read(buffer)

                                        if (total - lastSync > 25 * 1024 * 1024) {
                                            output.force(false)
                                            lastSync = total
                                            monitor(total)
                                        }
                                    }

                                    output.force(true)
                                    monitor(total)
                                }

                                val realChecksum = computeChecksum(dstTmpFilePath)

                                if (realChecksum != info.checksumSha256) {
                                    throw ChecksumMismatch(expected = info.checksumSha256, actual = realChecksum)
                                }

                                moveTmpToDestination(dstTmpFilePath, dstPath)

                                cleanup.cancel()

                                Date(dstPath.getLastModifiedTime().toMillis())
                            } finally {
                                lastAliveKeeper.cancel()
                            }
                        }
                    } catch (e: Throwable) {
                        withContext(NonCancellable) {
                            indexStore.removeIncomingFile(filename)
                            if (cleanup.files.isNotEmpty()) {
                                println("Not completed successfully, cleaning up: ${cleanup.files}")
                                cleanup.runPremature()
                            }
                        }
                        throw e
                    }

                indexStore.storeFile(
                    filename,
                    info.length,
                    lastModified,
                    info.checksumSha256,
                    dropIncomingFile = true,
                )
            } finally {
                withContext(NonCancellable) {
                    inProgressHandler.onEnd(dstPath)
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getMetadata(): RepositoryMetadata =
        withContext(Dispatchers.IO) {
            if (metadataPath.exists()) {
                metadataPath.inputStream().use {
                    Json.decodeFromStream<RepositoryMetadata>(it)
                }
            } else {
                if (archiveRoot.exists()) {
                    RepositoryMetadata()
                } else {
                    throw RuntimeException("Something went wrong")
                }
            }
        }

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        safeFileReadWrite(metadataPath) { oldString ->
            val old = oldString?.let { Json.decodeFromString(oldString) } ?: RepositoryMetadata()

            Json.encodeToString(transform(old))
        }
    }

    override suspend fun contains(path: String): Boolean =
        withContext(Dispatchers.IO) {
            val fullPath = root.resolve(safeSubPath(path))

            indexStore.file(path) != null && fullPath.isRegularFile()
        }

    override suspend fun index(): RepoIndex = indexFlow.firstLoadedOrFailure()

    private fun loadIgnorePatterns(): List<PathMatcher> {
        val ignorePatternsFile = root.resolve(ignorePatternsFileName)

        if (!ignorePatternsFile.isRegularFile()) {
            return emptyList()
        }

        val ignorePatternsLines = ignorePatternsFile.readText().lines()

        return ignorePatternsLines
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { FileSystems.getDefault().getPathMatcher("glob:$it") }
    }

    val throttlePauseDuration: Duration = 500.milliseconds

    override val indexFlow: StateFlow<Loadable<RepoIndex>> by lazy { initIndexFlow() }

    override val localIndex: Flow<Loadable<StatusOperation.Result>> by lazy { initLocalIndex() }

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        metadataPath
            .produceLoadableStateIn(
                scope,
                watchDispatcher = ioDispatcher,
                workDispatcher = ioDispatcher,
                message = "Repository metadata: $root",
                throttle = throttlePauseDuration,
            ) {
                if (metadataPath.exists()) {
                    Json.decodeFromString<RepositoryMetadata>(metadataPath.readText())
                } else {
                    if (archiveRoot.exists()) {
                        RepositoryMetadata()
                    } else {
                        throw RuntimeException("Something went wrong")
                    }
                }
            }

    protected suspend fun cancelScope() {
        scope.cancel()
        job.join()
    }

    override suspend fun close() {
        cancelScope()
    }
}
