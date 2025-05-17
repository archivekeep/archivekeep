package org.archivekeep.files.procedures.indexupdate

import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.Move

data class IndexUpdateMoveProgress(
    val movesToExecute: Set<Move>,
    val moved: Set<Move>,
    val error: Map<Move, Any>,
    val finished: Boolean,
)
