package org.archivekeep.files.procedures.reindex

data class FileReindexProgress(
    val filesToReindex: Set<String>,
    val reindexedFiles: Set<String>,
    val error: Map<String, Any>,
    val finished: Boolean,
)
