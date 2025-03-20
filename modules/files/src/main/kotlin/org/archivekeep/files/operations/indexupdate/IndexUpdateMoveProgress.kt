package org.archivekeep.files.operations.indexupdate

data class IndexUpdateMoveProgress(
    val moved: Set<AddOperation.PreparationResult.Move>,
    val error: Map<AddOperation.PreparationResult.Move, Any>,
    val finished: Boolean,
)
