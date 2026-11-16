package org.archivekeep.files.procedures.indexupdate

import java.io.PrintWriter
import java.nio.file.Path

class ImmediateIndexUpdateTextualProgressTracker(
    val out: PrintWriter,
    val fromArchiveToRelativePath: (path: String) -> Path = { Path.of(it) },
) : ImmediateIndexUpdateProgressTracker {
    override fun onAddCompleted(newFile: String) {
        out.println("added: ${fromArchiveToRelativePath(newFile)}")
    }

    override fun onMoveIgnored(
        newFile: String,
        oldLocations: List<String>,
    ) {
        out.println(
            "move ignored: ${fromArchiveToRelativePath(newFile)} (previously ${oldLocations.joinToString(", ") { fromArchiveToRelativePath(it).toString() }})",
        )
    }

    override fun onDuplicationIgnored(
        newFile: String,
        existingFiles: List<String>,
    ) {
        out.println(
            "duplication ignored: ${fromArchiveToRelativePath(
                newFile,
            )} (existing files: ${existingFiles.joinToString(", ") { fromArchiveToRelativePath(it).toString() }})",
        )
    }
}
