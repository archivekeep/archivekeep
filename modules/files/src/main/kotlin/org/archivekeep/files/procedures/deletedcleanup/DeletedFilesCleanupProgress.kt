package org.archivekeep.files.procedures.deletedcleanup

data class DeletedFilesCleanupProgress(
    val filesToRemove: Set<String>,
    val removedFiles: Set<String>,
    val error: Map<String, Any>,
    val finished: Boolean,
)
