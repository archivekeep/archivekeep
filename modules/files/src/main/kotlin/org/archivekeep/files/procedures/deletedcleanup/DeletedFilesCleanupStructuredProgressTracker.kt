package org.archivekeep.files.procedures.deletedcleanup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeletedFilesCleanupStructuredProgressTracker(
    filesToRemove: Set<String>,
) : DeletedFilesCleanupProgressTracker {
    private val deletedFilesCleanupProgressMutableFlow =
        MutableStateFlow(
            DeletedFilesCleanupProgress(
                filesToRemove,
                emptySet(),
                emptyMap(),
                false,
            ),
        )

    val fileRemoveProgressFlow = deletedFilesCleanupProgressMutableFlow.asStateFlow()

    override fun onFileRemoved(removedFile: String) {
        deletedFilesCleanupProgressMutableFlow.update {
            it.copy(
                removedFiles = it.removedFiles + setOf(removedFile),
            )
        }
    }
}
