package org.archivekeep.files.procedures.deletedcleanup

import java.io.PrintWriter
import java.nio.file.Path

class DeletedFilesCleanupTextualProgressTracker(
    val out: PrintWriter,
    val fromArchiveToRelativePath: (path: String) -> Path = { Path.of(it) },
) : DeletedFilesCleanupProgressTracker {
    override fun onFileRemoved(removedFile: String) {
        out.println("removed: ${fromArchiveToRelativePath(removedFile)}")
    }
}
