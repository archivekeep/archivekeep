package org.archivekeep.files.procedures.indexupdate

interface IndexUpdateProgressTracker {
    fun onMoveCompleted(move: IndexUpdateProcedure.DiscoveryResult.Move)

    fun onMovesFinished()

    fun onAddCompleted(newFile: String)

    fun onAddFinished()
}
