package org.archivekeep.files.repo.files

import computeChecksum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.archivekeep.files.exceptions.FileDoesntExist
import org.archivekeep.files.exceptions.NotRegularFilePath
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.ObservableWorkingRepo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.safeFileReadWrite
import safeSubPath
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.StandardOpenOption
import java.util.Collections.singletonList
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText
import kotlin.streams.asSequence

const val ignorePatternsFileName = ".archivekeepignore"

class FilesRepo(
    val root: Path,
    internal val archiveRoot: Path = root.resolve(".archive"),
    internal val checksumsRoot: Path = archiveRoot.resolve("checksums"),
) : LocalRepo {
    internal val metadataPath = root.resolve(".archive").resolve("metadata.json")

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

    override suspend fun storedFiles(): List<String> =
        Files
            .walk(checksumsRoot)
            .asSequence()
            .filter { it.isRegularFile() && it.extension == "sha256" }
            .map { it.relativeTo(checksumsRoot).invariantSeparatorsPathString.removeSuffix(".sha256") }
            .toList()
            .sorted()

    override suspend fun verifyFileExists(path: String): Boolean {
        val fullPath = root.resolve(safeSubPath(path))

        return fullPath.exists() && fullPath.isRegularFile()
    }

    override suspend fun fileChecksum(path: String): String {
        val fullChecksumPath = checksumsRoot.resolve("${safeSubPath(path)}.sha256")
        val checksumContents = fullChecksumPath.readText()

        return checksumContents.split(" ", limit = 2)[0]
    }

    override suspend fun computeFileChecksum(path: Path): String {
        val fullPath = root.resolve(safeSubPath(path))

        return computeChecksum(fullPath)
    }

    override suspend fun add(path: String) {
        val fullPath = root.resolve(safeSubPath(path))
        val checksum = computeChecksum(fullPath)

        storeFileChecksum(path, checksum)
    }

    override suspend fun delete(filename: String) {
        val fullPath = root.resolve(safeSubPath(filename))
        val checksumPath = getChecksumPath(filename)

        if (!fullPath.isRegularFile()) {
            throw NotRegularFilePath(fullPath.toString())
        }
        if (!checksumPath.isRegularFile()) {
            throw NotRegularFilePath(checksumPath.toString())
        }

        fullPath.deleteExisting()
        checksumPath.deleteExisting()
    }

    override suspend fun remove(path: String) {
        val checksumPath = getChecksumPath(path)

        if (!checksumPath.isRegularFile()) {
            throw NotRegularFilePath(checksumPath.toString())
        }

        checksumPath.deleteIfExists()
    }

    override suspend fun move(
        from: String,
        to: String,
    ) {
        val dstPath = root.resolve(safeSubPath(to))
        if (dstPath.exists()) {
            throw FileDoesntExist(to)
        }

        val checksum = this.fileChecksum(from)
        this.storeFileChecksum(to, checksum)

        dstPath.createParentDirectories()

        root.resolve(from).moveTo(dstPath)

        getChecksumPath(from).deleteExisting()
    }

    override suspend fun open(filename: String): Pair<ArchiveFileInfo, InputStream> {
        val checksum = fileChecksum(filename)
        val realPath = root.resolve(safeSubPath(filename))

        return Pair(
            ArchiveFileInfo(
                length = realPath.fileSize(),
                checksumSha256 = checksum,
            ),
            realPath.inputStream(),
        )
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
    ) {
        val dstPath = root.resolve(safeSubPath(filename))

        withContext(Dispatchers.IO) {
            dstPath.createParentDirectories()

            val fc =
                FileChannel.open(
                    dstPath,
                    StandardOpenOption.CREATE_NEW,
//                    StandardOpenOption.SYNC,
                    StandardOpenOption.WRITE,
                )

            val cleanup = UnfinishedStoreCleanup()

            try {
                println("copy to start: $dstPath")
                cleanup.files.add(dstPath)
                fc.use { output ->
                    val buffer = ByteArray(2 * 1024 * 1024)
                    var read: Int = stream.read(buffer)

                    while (read != -1) {
                        // check the job is active
                        yield()

                        output.write(ByteBuffer.wrap(buffer, 0, read))
                        read = stream.read(buffer)

                        // TODO - each 128MB: output.force(false)
                    }

                    output.force(true)
                }
                println("copy to end: $dstPath")

                val realChecksum = computeChecksum(dstPath)

                if (realChecksum != info.checksumSha256) {
                    throw RuntimeException("copied file has wrong checksum: got=$realChecksum, expected=${info.checksumSha256}")
                }

                cleanup.files.add(0, getChecksumPath(filename))
                storeFileChecksum(filename, info.checksumSha256)

                cleanup.cancel()
            } finally {
                withContext(NonCancellable) {
                    if (cleanup.files.isNotEmpty()) {
                        println("Not completed successfully, cleaning up: ${cleanup.files}")
                        cleanup.runPremature()
                    }
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

    private fun storeFileChecksum(
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

    override suspend fun contains(path: String): Boolean {
        val checksumPath = getChecksumPath(path)
        val fullPath = root.resolve(safeSubPath(path))

        return checksumPath.isRegularFile() && fullPath.isRegularFile()
    }

    private fun getChecksumPath(path: String) = Path("${checksumsRoot.resolve(safeSubPath(path))}.sha256")

    override suspend fun index(): RepoIndex {
        val files =
            Files
                .walk(checksumsRoot)
                .asSequence()
                .filter { it.isRegularFile() && it.extension == "sha256" }
                .map {
                    RepoIndex.File(
                        path = it.relativeTo(checksumsRoot).invariantSeparatorsPathString.removeSuffix(".sha256"),
                        checksumSha256 = it.readText().split(" ", limit = 2)[0],
                    )
                }.toList()

        return RepoIndex(files)
    }

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

    override val observable: ObservableWorkingRepo by lazy {
        ObservableFilesRepo(this)
    }
}

fun openFilesRepoOrNull(path: Path): FilesRepo? {
    val checksumDir = path.resolve(".archive").resolve("checksums")

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
