package org.archivekeep.core.repo.files

import computeChecksum
import org.archivekeep.core.exceptions.NotRegularFilePath
import org.archivekeep.core.repo.LocalRepo
import org.archivekeep.core.repo.RepoIndex
import safeSubPath
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.StandardOpenOption
import java.util.Collections.singletonList
import kotlin.io.path.*

const val ignorePatternsFileName = ".archivekeepignore"

class FilesRepo(
    private val root: Path,

    private val checksumsRoot: Path = root.resolve(".archive").resolve("checksums")
) : LocalRepo {

    override fun findAllFiles(globs: List<String>): List<Path> {
        val matchers = globs
            .map {
                // TODO: add check for escaping archive root
                val absolutePath = root.resolve(it).normalize().toString()
                FileSystems.getDefault().getPathMatcher("glob:$absolutePath")
            }

        val ignorePatterns = loadIgnorePatterns()

        return Files.walk(root)
            .filter { path: Path? -> if (path != null) matchers.any { it.matches(path) } else false }
            .flatMap {
                if (it.isRegularFile())
                    singletonList(it).stream()
                else
                    Files.walk(it).filter { it.isRegularFile() }
            }
            .map { it.relativeTo(root) }
            .filter { path ->
                val parts = path
                    .invariantSeparatorsPathString
                    .split("/")

                parts[0] != ".archive" && parts[0] != ignorePatternsFileName && parts.none { part ->
                    ignorePatterns.any {
                        it.matches(
                            Path(part)
                        )
                    }
                }
            }
            .toList()
    }

    override fun storedFiles(): List<String> {
        return Files.walk(checksumsRoot)
            .filter { it.isRegularFile() && it.extension == "sha256" }
            .map { it.relativeTo(checksumsRoot).invariantSeparatorsPathString.removeSuffix(".sha256") }
            .toList()
            .sorted()
    }

    override fun verifyFileExists(path: String): Boolean {
        val fullPath = root.resolve(safeSubPath(path))

        return fullPath.exists() && fullPath.isRegularFile()
    }

    override fun fileChecksum(path: String): String {
        val fullChecksumPath = checksumsRoot.resolve("${safeSubPath(path)}.sha256")
        val checksumContents = fullChecksumPath.readText()

        return checksumContents.split(" ", limit = 2)[0]
    }

    override fun computeFileChecksum(path: Path): String {
        val fullPath = root.resolve(safeSubPath(path))

        return computeChecksum(fullPath)
    }

    override fun add(path: String) {
        val fullPath = root.resolve(safeSubPath(path))
        val checksum = computeChecksum(fullPath)

        storeFileChecksum(path, checksum)
    }

    override fun remove(path: String) {
        val checksumPath = Path("${checksumsRoot.resolve(safeSubPath(path))}.sha256")

        if (!checksumPath.isRegularFile()) {
            throw NotRegularFilePath(checksumPath.toString())
        }

        checksumPath.deleteIfExists()
    }

    private fun storeFileChecksum(path: String, checksum: String) {
        val checksumPath = Path("${checksumsRoot.resolve(safeSubPath(path))}.sha256")

        checksumPath.createParentDirectories()

        checksumPath.writeText(
            "$checksum ${Path(path).invariantSeparatorsPathString}\n",
            options = arrayOf(
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.SYNC,
            )
        )
    }

    override fun contains(path: String): Boolean {
        val checksumPath = Path("${checksumsRoot.resolve(safeSubPath(path))}.sha256")
        val fullPath = root.resolve(safeSubPath(path))

        return checksumPath.isRegularFile() && fullPath.isRegularFile()
    }

    override fun index(): RepoIndex {
        val files = Files.walk(checksumsRoot)
            .filter { it.isRegularFile() && it.extension == "sha256" }
            .map {
                RepoIndex.File(
                    path = it.relativeTo(checksumsRoot).invariantSeparatorsPathString.removeSuffix(".sha256"),
                    checksumSha256 = it.readText().split(" ", limit = 2)[0]
                )
            }
            .toList()

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
}

fun openFilesRepoOrNull(path: Path): FilesRepo? {
    val checksumDir = path.resolve(".archive").resolve("checksums")

    if (checksumDir.isDirectory()) {
        return FilesRepo(
            path
        )
    }

    return null
}
