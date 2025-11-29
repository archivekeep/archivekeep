package org.archivekeep.files.repo.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.archivekeep.files.exceptions.ChecksumMismatch
import org.archivekeep.files.exceptions.DestinationExists
import org.archivekeep.files.exceptions.NotRegularFilePath
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.coroutines.flowScopedToThisJob
import org.archivekeep.utils.flows.logLoadableResourceLoad
import org.archivekeep.utils.io.watchForSingleFile
import org.archivekeep.utils.io.watchRecursively
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.loading.produceLoadable
import org.archivekeep.utils.loading.produceLoadableStateIn
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.safeFileReadWrite
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.StandardOpenOption
import java.util.Collections.singletonList
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val ignorePatternsFileName = ".archivekeepignore"

const val checksumsSubDir = "checksums"

class FilesRepo(
    val root: Path,
    internal val archiveRoot: Path = root.resolve(".archive"),
    checksumsRoot: Path = archiveRoot.resolve(checksumsSubDir),
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocalRepo {
    private val scope = CoroutineScope(SupervisorJob() + CoroutineName("AAA") + stateDispatcher)

    private val metadataPath = root.resolve(".archive").resolve("metadata.json")

    private val inProgressHandler = InProgressHandler(scope)

    private val indexStore =
        FilesystemIndexStore(
            checksumsRoot,
            activeJobFlow = inProgressHandler.jobActiveOnIdleDelayedStart,
            fileSizeProvider = ::getFileSize,
            ioDispatcher = ioDispatcher,
        )

    override fun getFileSize(filename: String): Long? {
        val path = root.resolve(safeSubPath(filename))

        return if (path.exists()) path.fileSize() else null
    }

    override suspend fun findAllFiles(globs: List<String>): List<Path> {
        val matchers =
            globs
                .map {
                    // TODO: add check for escaping archive root
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

    override suspend fun indexedFilenames(): List<String> = indexStore.all()

    override suspend fun verifyFileExists(path: String): Boolean {
        val fullPath = root.resolve(safeSubPath(path))

        return fullPath.exists() && fullPath.isRegularFile()
    }

    override suspend fun fileChecksum(path: String): String = indexStore.fileChecksum(path)

    override suspend fun computeFileChecksum(path: Path): String {
        val fullPath = root.resolve(safeSubPath(path))

        return computeChecksum(fullPath)
    }

    override suspend fun add(path: String) {
        val fullPath = root.resolve(safeSubPath(path))
        val checksum = computeChecksum(fullPath)

        indexStore.storeFileChecksum(path, checksum)
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

            val move = indexStore.beginMove(from, to)

            dstPath.createParentDirectories()
            root.resolve(from).moveTo(dstPath)

            move.completed()
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

        withContext(Dispatchers.IO) {
            dstPath.createParentDirectories()

            val fc =
                try {
                    FileChannel.open(
                        dstPath,
                        StandardOpenOption.CREATE_NEW,
//                    StandardOpenOption.SYNC,
                        StandardOpenOption.WRITE,
                    )
                } catch (e: FileAlreadyExistsException) {
                    throw DestinationExists(filename, cause = e)
                }

            val cleanup = UnfinishedStoreCleanup()

            try {
                inProgressHandler.onStart(dstPath)
                cleanup.files.add(dstPath)
                println("copy to start: $dstPath")
                fc.use { output ->
                    val buffer = ByteArray(2 * 1024 * 1024)
                    var read: Int = stream.read(buffer)
                    var total: Long = 0
                    var lastSync: Long = 0

                    while (read != -1) {
                        // check the job is active
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
                println("copy to end: $dstPath")

                val realChecksum = computeChecksum(dstPath)

                if (realChecksum != info.checksumSha256) {
                    throw ChecksumMismatch(expected = info.checksumSha256, actual = realChecksum)
                }

                indexStore.storeChecksumForSave(cleanup, filename, info.checksumSha256)

                cleanup.cancel()
            } finally {
                withContext(NonCancellable) {
                    if (cleanup.files.isNotEmpty()) {
                        println("Not completed successfully, cleaning up: ${cleanup.files}")
                        cleanup.runPremature()
                    }
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

            println("Updating")

            Json.encodeToString(transform(old))
        }
    }

    override suspend fun contains(path: String): Boolean =
        withContext(Dispatchers.IO) {
            val fullPath = root.resolve(safeSubPath(path))

            indexStore.contains(path) && fullPath.isRegularFile()
        }

    override suspend fun index(): RepoIndex = indexStore.index()

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

    @OptIn(FlowPreview::class)
    private val calculationCause =
        root
            .watchRecursively(ioDispatcher)
            .debounce(100.milliseconds)
            .map { "update" }
            .shareIn(scope, SharingStarted.WhileSubscribed(), 0)
            .onStart { emit("start on index change") }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val localIndex: Flow<Loadable<StatusOperation.Result>> =
        indexStore
            .indexFlow
            .flatMapLoadableFlow { index ->
                val indexFiles = index.files.map { it.path }.toSet()

                inProgressHandler
                    .jobActiveOnIdleDelayedStart
                    .flowScopedToThisJob {
                        calculationCause
                            .conflate()
                            .produceLoadable(
                                ioDispatcher,
                                "Local status: $root",
                            ) {
                                val allFiles =
                                    findAllFiles(listOf("*"))
                                        .map {
                                            it.invariantSeparatorsPathString
                                        }.sorted()

                                currentCoroutineContext().ensureActive()

                                StatusOperation.Result(
                                    newFiles = allFiles.filter { !indexFiles.contains(it) },
                                    indexedFiles = allFiles.filter { indexFiles.contains(it) },
                                )
                            }
                    }
            }.logLoadableResourceLoad("Local status: $root")
            .flowOn(ioDispatcher)
            .stateIn(scope)

    override val indexFlow = indexStore.indexFlow

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        metadataPath
            .watchForSingleFile(ioDispatcher)
            .map { "update" }
            .onStart { emit("start") }
            .conflate()
            .produceLoadableStateIn(
                scope,
                ioDispatcher,
                "Repository metadata: $root",
                throttlePauseDuration,
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

    @OptIn(ExperimentalPathApi::class)
    suspend fun deinitialize() {
        withContext(ioDispatcher) {
            archiveRoot.deleteRecursively()
        }
    }
}

fun openFilesRepoOrNull(path: Path): FilesRepo? {
    val checksumDir = path.resolve(".archive").resolve(checksumsSubDir)

    if (checksumDir.isDirectory()) {
        return FilesRepo(
            path,
        )
    }

    return null
}

fun createFilesRepo(path: Path): FilesRepo {
    val archiveDir = path.resolve(".archive")
    val checksumDir = archiveDir.resolve("checksums")

    if (checksumDir.isDirectory()) {
        throw RuntimeException("Already exists")
    }

    archiveDir.createDirectory()
    checksumDir.createDirectory()

    return FilesRepo(path)
}
