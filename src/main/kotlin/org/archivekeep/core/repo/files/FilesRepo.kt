package org.archivekeep.core.repo.files

import org.archivekeep.core.repo.LocalRepo
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
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

                parts[0] != ".archive" && parts[0] != ignorePatternsFileName && parts.none { part -> ignorePatterns.any { it.matches(Path(part)) } }
            }
            .toList()
    }

    override fun contains(filename: String): Boolean {
        // TODO: check filename doesn't escape archive

        return checksumsRoot.resolve("$filename.sha256").isRegularFile()
                && root.resolve(filename).isRegularFile()
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