package org.archivekeep.files.operations.indexupdate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AddOperationProgressTracker {
    private val addProgressMutableFlow = MutableStateFlow(IndexUpdateAddProgress(emptySet(), emptyMap(), false))
    private val moveProgressMutableFlow = MutableStateFlow(IndexUpdateMoveProgress(emptySet(), emptyMap(), false))

    val addProgressFlow = addProgressMutableFlow.asStateFlow()
    val moveProgressFlow = moveProgressMutableFlow.asStateFlow()

    public fun onMoveCompleted(move: AddOperation.PreparationResult.Move) {
        moveProgressMutableFlow.update {
            it.copy(
                moved = it.moved + setOf(move),
            )
        }
    }

    fun onMovesFinished() {
        moveProgressMutableFlow.update {
            it.copy(finished = true)
        }
    }

    fun onAddCompleted(newFile: String) {
        addProgressMutableFlow.update {
            it.copy(
                added = it.added + setOf(newFile),
            )
        }
    }

    fun onAddFinished() {
        addProgressMutableFlow.update {
            it.copy(finished = true)
        }
    }
}
