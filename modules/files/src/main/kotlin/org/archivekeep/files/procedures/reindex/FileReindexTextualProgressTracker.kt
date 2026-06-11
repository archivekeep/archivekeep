package org.archivekeep.files.procedures.reindex

import java.io.PrintWriter
import java.nio.file.Path

class FileReindexTextualProgressTracker(
    val out: PrintWriter,
    val fromArchiveToRelativePath: (path: String) -> Path = { Path.of(it) },
) : FileReindexProgressTracker {
    override fun onFileReindexed(reindexedFile: String) {
        out.println("reindexed: ${fromArchiveToRelativePath(reindexedFile)}")
    }
}
