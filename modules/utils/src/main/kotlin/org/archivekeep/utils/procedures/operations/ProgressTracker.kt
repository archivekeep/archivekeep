package org.archivekeep.utils.procedures.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.archivekeep.utils.flows.delayReductions

private class Key

class ProgressTracker(
    val sharingScope: CoroutineScope,
) {
    private val inProgressOperationsStatsMutable =
        MutableStateFlow(emptyMap<Key, OperationProgress>())

    val inProgressOperationsProgressFlow =
        inProgressOperationsStatsMutable
            .map { it.values.toList() }
            // to prevent blinking of white-space
            .delayReductions()
            .stateIn(sharingScope, SharingStarted.WhileSubscribed(), emptyList())

    suspend fun runWithTracker(block: suspend (progressReport: (OperationProgress) -> Unit) -> Unit) {
        val key = Key()

        fun progressReport(progress: OperationProgress) {
            inProgressOperationsStatsMutable.update {
                it.toMutableMap().apply { set(key, progress) }.toMap()
            }
        }

        try {
            block(::progressReport)
        } finally {
            inProgressOperationsStatsMutable.update {
                it.toMutableMap().apply { remove(key) }.toMap()
            }
        }
    }
}
