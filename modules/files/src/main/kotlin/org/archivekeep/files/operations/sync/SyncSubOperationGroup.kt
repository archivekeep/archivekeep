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

        subOperations.forEach { operation ->
            if (limitToSubset != null && !limitToSubset.contains(operation)) {
                return@forEach
            }

            operation.apply(base, dst, logger)

            completedSteps = completedSteps + listOf(operation)
            progressReport(
                constructProgress(subOperations, completedSteps),
            )

            yield()
        }

        return constructProgress(subOperations, completedSteps)
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
