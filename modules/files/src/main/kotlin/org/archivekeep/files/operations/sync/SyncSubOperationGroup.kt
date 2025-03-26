package org.archivekeep.files.operations.sync

import kotlinx.coroutines.yield
import org.archivekeep.files.repo.Repo

sealed class SyncSubOperationGroup<T : SyncSubOperation>(
    val subOperations: List<T>,
) {
    suspend fun execute(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        progressReport: (progress: Progress) -> Unit,
        limitToSubset: Set<SyncSubOperation>?,
    ): Progress {
        var completedSteps = emptyList<T>()

        val operationsToExecute =
            if (limitToSubset == null) {
                subOperations
            } else {
                subOperations.filter { it in limitToSubset }
            }

        operationsToExecute.forEach { operation ->
            operation.apply(base, dst, logger)

            completedSteps = completedSteps + listOf(operation)
            progressReport(
                constructProgress(operationsToExecute, completedSteps),
            )

            yield()
        }

        return constructProgress(operationsToExecute, completedSteps)
    }

    fun isNoOp(): Boolean = subOperations.isEmpty()

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
