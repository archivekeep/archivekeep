package org.archivekeep.files.procedures.indexupdate

interface IndexUpdateProgressTracker {
    fun onMoveCompleted(move: IndexUpdateProcedure.PreparationResult.Move)

    fun onMovesFinished()

    fun onAddCompleted(newFile: String)

    fun onAddFinished()
}
