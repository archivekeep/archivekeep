package org.archivekeep.files.procedures.reindex

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FileReindexStructuredProgressTracker(
    filesToReindex: Set<String>,
) : FileReindexProgressTracker {
    private val fileReindexProgressMutableFlow =
        MutableStateFlow(
            FileReindexProgress(
                filesToReindex,
                emptySet(),
                emptyMap(),
                false,
            ),
        )

    val fileReindexProgressFlow = fileReindexProgressMutableFlow.asStateFlow()

    override fun onFileReindexed(reindexedFile: String) {
        fileReindexProgressMutableFlow.update {
            it.copy(
                reindexedFiles = it.reindexedFiles + setOf(reindexedFile),
            )
        }
    }
}
