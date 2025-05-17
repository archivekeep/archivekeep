package org.archivekeep.files.procedures.indexupdate

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.Move

class IndexUpdateStructuredProgressTracker(
    filesToAdd: Set<String>,
    movesToExecute: Set<Move>,
) : IndexUpdateProgressTracker {
    private val addProgressMutableFlow = MutableStateFlow(IndexUpdateAddProgress(filesToAdd, emptySet(), emptyMap(), false))
    private val moveProgressMutableFlow = MutableStateFlow(IndexUpdateMoveProgress(movesToExecute, emptySet(), emptyMap(), false))

    val addProgressFlow = addProgressMutableFlow.asStateFlow()
    val moveProgressFlow = moveProgressMutableFlow.asStateFlow()

    override fun onMoveCompleted(move: Move) {
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
