package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import org.archivekeep.files.procedures.progress.BytesPerSecond
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import kotlin.time.Duration.Companion.seconds

class SyncProgressTracker<T : SyncOperation>(
    val createProgress: (completed: List<T>) -> SyncProcedureJobTask.ProgressSummary<T>,
) {
    private val sharingScope = CoroutineScope(SupervisorJob())

    private val speed = MutableStateFlow<BytesPerSecond?>(null)
    private val completedOperations = MutableStateFlow(emptyList<T>())

    val executionProgressSummaryStateFlow =
        combine(
            speed,
            completedOperations,
        ) { speed, completedOperations ->
            var result = createProgress(completedOperations)

            if (speed != null) {
                val bytesToCopy =
                    (result.allOperations.toSet() - result.completedOperations.toSet())
                        .map { it.bytesToCopy }
                        .fold(0L as Long?) { acc, it ->
                            if (acc != null && it != null) acc + it else null
                        }

                if (bytesToCopy != null) {
                    result =
                        result.copy(
                            timeEstimated =
                                if (speed.value > 0) {
                                    (bytesToCopy / speed.value).seconds
                                } else {
                                    null
                                },
                        )
                }
            }

            result
        }.stateIn(sharingScope, SharingStarted.WhileSubscribed(), createProgress(emptyList()))

    suspend fun runOverride(
        parentProcedureContext: ProcedureExecutionContext,
        function: suspend (ProcedureExecutionContext, (T) -> Unit) -> Unit,
    ) {
        val flowScope = CoroutineScope(currentCoroutineContext() + SupervisorJob(currentCoroutineContext().job))
        val modifiedContext = SyncProgressTracingSubContext(flowScope, parentProcedureContext)

        modifiedContext
            .speedFlow
            .cancellable()
            .onEach { speed.value = it }
            .onCompletion { speed.value = null }
            .launchIn(flowScope)

        function(modifiedContext) { operation ->
            completedOperations.update { it + listOf(operation) }
        }

        flowScope.cancel()
    }
}
