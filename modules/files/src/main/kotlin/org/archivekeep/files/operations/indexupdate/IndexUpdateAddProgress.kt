package org.archivekeep.files.operations.indexupdate

data class IndexUpdateAddProgress(
    val added: Set<String>,
    val error: Map<String, Any>,
    val finished: Boolean,
)
