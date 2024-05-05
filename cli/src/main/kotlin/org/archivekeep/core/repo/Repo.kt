package org.archivekeep.core.repo

import java.io.InputStream
import java.nio.file.Path

data class RepoIndex (
    val files: List<File>
) {
    val byChecksumSha256 = files.groupBy { it.checksumSha256 }

    val byPath = files.associateBy { it.path }

    data class File (
        val path: String,
        val checksumSha256: String
    )
}

data class ArchiveFileInfo (
    val checksumSha256: String
)

interface Repo {
    fun contains(path: String): Boolean

    fun index(): RepoIndex

    fun move(from: String, to: String)
    fun open(filename: String): Pair<ArchiveFileInfo, InputStream>
    fun save(filename: String, info: ArchiveFileInfo, stream: InputStream)
}

interface LocalRepo: Repo {
    fun findAllFiles(globs: List<String>): List<Path>

    fun storedFiles(): List<String>
    fun verifyFileExists(path: String): Boolean
    fun fileChecksum(path: String): String
    fun computeFileChecksum(path: Path): String


    fun add(path: String)
    fun remove(path: String)

}

