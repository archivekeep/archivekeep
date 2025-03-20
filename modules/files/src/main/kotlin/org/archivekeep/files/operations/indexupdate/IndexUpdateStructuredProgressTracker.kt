package org.archivekeep.files.operations.indexupdate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class IndexUpdateStructuredProgressTracker : IndexUpdateProgressTracker {
    private val addProgressMutableFlow = MutableStateFlow(IndexUpdateAddProgress(emptySet(), emptyMap(), false))
    private val moveProgressMutableFlow = MutableStateFlow(IndexUpdateMoveProgress(emptySet(), emptyMap(), false))

    val addProgressFlow = addProgressMutableFlow.asStateFlow()
    val moveProgressFlow = moveProgressMutableFlow.asStateFlow()

    override fun onMoveCompleted(move: AddOperation.PreparationResult.Move) {
        moveProgressMutableFlow.update {
            it.copy(
                moved = it.moved + setOf(move),
            )
        }
    }

    override fun onMovesFinished() {
        moveProgressMutableFlow.update {
            it.copy(finished = true)
        }
    }

    override fun onAddCompleted(newFile: String) {
        addProgressMutableFlow.update {
            it.copy(
                added = it.added + setOf(newFile),
            )
        }
    }

    override fun onAddFinished() {
        addProgressMutableFlow.update {
            it.copy(finished = true)
        }
    }
}
