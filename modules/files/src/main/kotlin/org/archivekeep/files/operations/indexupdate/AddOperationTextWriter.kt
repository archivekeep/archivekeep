package org.archivekeep.files.operations.indexupdate

import java.io.PrintWriter
import java.nio.file.Path

class AddOperationTextWriter(
    val out: PrintWriter,
    val fromArchiveToRelativePath: (path: String) -> Path = { Path.of(it) },
) {
    public fun onMoveCompleted(move: AddOperation.PreparationResult.Move) {
        out.println(
            "moved: ${fromArchiveToRelativePath(move.from)} to ${
                fromArchiveToRelativePath(
                    move.to,
                )
            }",
        )
    }

    public fun onAddCompleted(newFile: String) {
        out.println("added: ${fromArchiveToRelativePath(newFile)}")
    }
}
