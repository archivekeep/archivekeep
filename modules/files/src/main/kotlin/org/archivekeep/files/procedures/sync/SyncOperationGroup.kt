package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.yield
import org.archivekeep.files.procedures.progress.OperationProgress
import org.archivekeep.files.repo.Repo

sealed class SyncOperationGroup<T : SyncOperation>(
    val operations: List<T>,
) {
    suspend fun execute(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        operationProgressMutableFlow: MutableStateFlow<List<OperationProgress>>,
        progressReport: (progress: Progress) -> Unit,
        limitToSubset: Set<SyncOperation>?,
    ): Progress {
        var completedSteps = emptyList<T>()

        val operationsToExecute =
            if (limitToSubset == null) {
                operations
            } else {
                operations.filter { it in limitToSubset }
            }

        operationsToExecute.forEach { operation ->
            operation.apply(base, dst, logger, operationProgressMutableFlow)

            completedSteps = completedSteps + listOf(operation)
            progressReport(
                constructProgress(operationsToExecute, completedSteps),
            )

            yield()
        }

        return constructProgress(operationsToExecute, completedSteps)
    }

    fun isNoOp(): Boolean = operations.isEmpty()

    abstract fun constructProgress(
        all: List<T>,
        completed: List<T>,
    ): Progress

    sealed interface Progress {
        // TODO: localization
        fun summaryText(): String

        fun progress(): Float
    }
}
