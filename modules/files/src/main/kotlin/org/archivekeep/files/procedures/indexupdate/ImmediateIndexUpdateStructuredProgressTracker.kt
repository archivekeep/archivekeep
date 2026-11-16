package org.archivekeep.files.procedures.indexupdate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ImmediateIndexUpdateStructuredProgressTracker(
    filesToProcess: Set<String>,
) : ImmediateIndexUpdateProgressTracker {
    private val mutableProgressFlow = MutableStateFlow(ImmediateIndexUpdateProgress(filesToProcess, emptySet(), emptyMap(), emptyMap(), emptyMap(), false))

    val progressFlow = mutableProgressFlow.asStateFlow()

    override fun onAddCompleted(newFile: String) {
        mutableProgressFlow.update {
            it.copy(
                addedNew = it.addedNew + setOf(newFile),
            )
        }
    }

    override fun onMoveIgnored(
        newFile: String,
        oldLocations: List<String>,
    ) {
        mutableProgressFlow.update {
            it.copy(
                discoveredMoves = it.discoveredMoves + mapOf(newFile to oldLocations),
            )
        }
    }

    override fun onDuplicationIgnored(
        newFile: String,
        existingFiles: List<String>,
    ) {
        mutableProgressFlow.update {
            it.copy(
                discoveredDuplicates = it.discoveredDuplicates + mapOf(newFile to existingFiles),
            )
        }
    }
}
