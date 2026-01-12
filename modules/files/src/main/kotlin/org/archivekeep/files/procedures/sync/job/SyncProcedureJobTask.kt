package org.archivekeep.files.procedures.sync.job

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.procedures.sync.discovery.DiscoveredSyncOperationsGroup
import org.archivekeep.files.procedures.sync.job.observation.SyncExecutionObserver
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.files.procedures.sync.operations.SyncOperation.ExecutionResult.FAIL
import org.archivekeep.files.procedures.sync.operations.SyncOperation.ExecutionResult.SUCCESS
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import org.archivekeep.utils.procedures.tasks.ProcedureJobTask
import org.archivekeep.utils.procedures.tasks.TaskExecutionProgressSummary
import kotlin.time.Duration

class SyncProcedureJobTask<T : SyncOperation>(
    private val discoveredSyncOperationsGroup: DiscoveredSyncOperationsGroup<T>,
    private val operationsToExecute: List<T>,
) : ProcedureJobTask<SyncProcedureJobTask.Context> {
    data class ProgressSummary<T : SyncOperation>(
        val discoveryOperationGroup: DiscoveredSyncOperationsGroup<T>,
        val allOperations: List<T>,
        val completedOperations: List<T>,
        override val timeEstimated: Duration?,
    ) : TaskExecutionProgressSummary.Simple {
        override val completion: Float =
            if (completedOperations.isNotEmpty()) {
                allOperations.size.toFloat() / completedOperations.size
            } else {
                0f
            }
    }

    data class Context(
        val context: ProcedureExecutionContext,
        val base: Repo,
        val dst: Repo,
        val observer: SyncExecutionObserver,
        val prompter: suspend (step: DiscoveredSyncOperationsGroup<*>) -> Boolean,
    )

    fun createProgress(completed: List<T>) =
        ProgressSummary(
            discoveredSyncOperationsGroup,
            operationsToExecute,
            completed,
            null,
        )

    private val tracker = SyncProgressTracker(::createProgress)

    override val executionProgressSummaryStateFlow: StateFlow<TaskExecutionProgressSummary>
        get() = tracker.executionProgressSummaryStateFlow

    override suspend fun execute(context: Context) {
        val confirmed = context.prompter(discoveredSyncOperationsGroup)

        if (!confirmed) {
            throw RuntimeException("abandoned")
        }

        val (parentProcedureContext, base, dst, logger) = context

        tracker.runOverride(parentProcedureContext) { modifiedContext, reportCompleted ->
            operationsToExecute.forEach { operation ->
                val result = operation.apply(modifiedContext, base, dst, logger)

                when (result) {
                    SUCCESS -> reportCompleted(operation)

                    FAIL -> {
                        // TODO: implement
                    }
                }

                yield()
            }
        }
    }
}
