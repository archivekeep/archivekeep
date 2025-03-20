package org.archivekeep.files.operations.indexupdate

interface IndexUpdateProgressTracker {
    fun onMoveCompleted(move: AddOperation.PreparationResult.Move)

    fun onMovesFinished()

    fun onAddCompleted(newFile: String)

    fun onAddFinished()
}
