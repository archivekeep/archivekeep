package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.archivekeep.files.exceptions.NotRegularFilePath
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.utils.coroutines.flowScopedToThisJob
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.flows.logLoadableResourceLoad
import org.archivekeep.utils.io.AutomaticFileCleanup
import org.archivekeep.utils.io.watchRecursively
import org.archivekeep.utils.loading.produceLoadable
import org.archivekeep.utils.loading.stateIn
import org.jetbrains.annotations.Blocking
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class FilesystemIndexStore(
    internal val checksumsRoot: Path,
    val scope: CoroutineScope,
    val activeJobFlow: Flow<Job?>,
    val fileSizeProvider: (filename: String) -> Long?,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val pollDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val throttlePauseDuration: Duration = 500.milliseconds

    @OptIn(FlowPreview::class)
    private val calculationCause =
        checksumsRoot
            .watchRecursively(ioDispatcher, pollDispatcher)
            .debounce(100.milliseconds)
            .map { "update" }
            .shareResourceIn(scope)
            .onStart { emit("start") }

    val indexFlow =
        activeJobFlow
            .flowScopedToThisJob {
                calculationCause
                    .conflate()
                    .produceLoadable(
                        ioDispatcher,
                        "Repository index: $checksumsRoot",
                        throttle = throttlePauseDuration,
                    ) {
                        index()
                    }
            }.logLoadableResourceLoad("Repository index: $checksumsRoot")
            .stateIn(scope)

    @Blocking
    fun contains(path: String): Boolean {
        val checksumPath = getChecksumPath(path)

        return checksumPath.isRegularFile()
    }

    @Blocking
    fun index(): RepoIndex {
        val files =
            Files
                .walk(checksumsRoot)
                .asSequence()
                .filter { it.isRegularFile() && it.extension == "sha256" }
                .map {
                    val path = it.relativeTo(checksumsRoot).invariantSeparatorsPathString.removeSuffix(".sha256")

                    RepoIndex.File(
                        path = path,
                        size = fileSizeProvider(path),
                        checksumSha256 = it.readText().split(" ", limit = 2)[0],
                    )
                }.sortedBy { it.path }
                .toList()

        return RepoIndex(files)
    }

    @Blocking
    fun all(): List<String> =
        Files
            .walk(checksumsRoot)
            .asSequence()
            .filter { it.isRegularFile() && it.extension == "sha256" }
            .map { it.relativeTo(checksumsRoot).invariantSeparatorsPathString.removeSuffix(".sha256") }
            .toList()
            .sorted()

    @Blocking
    fun fileChecksum(path: String): String {
        val fullChecksumPath = checksumsRoot.resolve("${safeSubPath(path)}.sha256")
        val checksumContents = fullChecksumPath.readText()

        return checksumContents.split(" ", limit = 2)[0]
    }

    @Blocking
    fun remove(path: String) {
        val checksumPath = getChecksumPath(path)

        if (!checksumPath.isRegularFile()) {
            throw NotRegularFilePath(checksumPath.toString())
        }

        checksumPath.deleteIfExists()
    }

    @Blocking
    fun beginMove(
        from: String,
        to: String,
    ): PendingMove {
        val checksum = this.fileChecksum(from)
        storeFileChecksum(to, checksum)

        return PendingMove(from, to)
    }

    inner class PendingMove(
        val from: String,
        to: String,
    ) {
        @Blocking
        fun completed() {
            getChecksumPath(from).deleteExisting()
        }
    }

    fun storeChecksumForSave(
        cleanup: AutomaticFileCleanup,
        filename: String,
        checksumSha256: String,
    ) {
        cleanup.files.add(0, getChecksumPath(filename))
        storeFileChecksum(filename, checksumSha256)
    }

    fun storeFileChecksum(
        path: String,
        checksum: String,
    ) {
        val checksumPath = getChecksumPath(path)

        checksumPath.createParentDirectories()

        checksumPath.writeText(
            "$checksum ${Path(path).invariantSeparatorsPathString}\n",
            options =
                arrayOf(
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.SYNC,
                ),
        )
    }

    private fun getChecksumPath(path: String) = Path("${checksumsRoot.resolve(safeSubPath(path))}.sha256")
}
