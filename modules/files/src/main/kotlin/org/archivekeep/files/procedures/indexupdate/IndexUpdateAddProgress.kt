package org.archivekeep.files.procedures.indexupdate

data class IndexUpdateAddProgress(
    val filesToAdd: Set<String>,
    val added: Set<String>,
    val error: Map<String, Any>,
    val finished: Boolean,
)
