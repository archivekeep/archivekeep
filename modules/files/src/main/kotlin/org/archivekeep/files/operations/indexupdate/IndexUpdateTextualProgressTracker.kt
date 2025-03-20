package org.archivekeep.files.operations.indexupdate

import java.io.PrintWriter
import java.nio.file.Path

class IndexUpdateTextualProgressTracker(
    val out: PrintWriter,
    val fromArchiveToRelativePath: (path: String) -> Path = { Path.of(it) },
) : IndexUpdateProgressTracker {
    override fun onMoveCompleted(move: AddOperation.PreparationResult.Move) {
        out.println(
            "moved: ${fromArchiveToRelativePath(move.from)} to ${
                fromArchiveToRelativePath(
                    move.to,
                )
            }",
        )
    }

    override fun onAddCompleted(newFile: String) {
        out.println("added: ${fromArchiveToRelativePath(newFile)}")
    }

    override fun onMovesFinished() {
        out.println("finished moving files")
    }

    override fun onAddFinished() {
        out.println("finished adding files")
    }
}
