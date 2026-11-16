package org.archivekeep.files.procedures.indexupdate

data class ImmediateIndexUpdateProgress(
    val filesToAdd: Set<String>,
    val addedNew: Set<String>,
    val error: Map<String, Any>,
    val discoveredDuplicates: Map<String, List<String>>,
    val discoveredMoves: Map<String, List<String>>,
    val finished: Boolean,
)
