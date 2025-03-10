package org.archivekeep

import org.archivekeep.utils.sha256
import java.io.File
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText

fun createArchiveWithContents(
    archiveTempDir: File,
    files: Map<String, String>,
) {
    createUnindexedFiles(archiveTempDir, files)

    files.forEach {
        val checksumPath = archiveTempDir.resolve(".archive/checksums").resolve(it.key + ".sha256").toPath()
        checksumPath.createParentDirectories()

        checksumPath.writeText(it.value.sha256())
    }
}

fun createUnindexedFiles(
    archiveTempDir: File,
    files: Map<String, String>,
) {
    files.forEach {
        val filePath = archiveTempDir.resolve(it.key).toPath()
        filePath.createParentDirectories()

        filePath.writeText(it.value)
    }
}
