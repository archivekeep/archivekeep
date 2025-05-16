package org.archivekeep.files.procedures.indexupdate

data class IndexUpdateMoveProgress(
    val moved: Set<IndexUpdateProcedure.PreparationResult.Move>,
    val error: Map<IndexUpdateProcedure.PreparationResult.Move, Any>,
    val finished: Boolean,
)
