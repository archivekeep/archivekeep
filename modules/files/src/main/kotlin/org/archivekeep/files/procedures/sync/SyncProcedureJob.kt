package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext

class SyncProcedureJob(
    val steps: List<SyncOperationGroup<*>>,
    val base: Repo,
    val dst: Repo,
    val prompter: suspend (step: SyncOperationGroup<*>) -> Boolean,
    val logger: SyncLogger,
    val limitToSubset: Set<SyncOperation>? = null,
) : AbstractProcedureJob() {
    val progress = MutableStateFlow(emptyList<SyncOperationGroup.Progress>())

    val progressReport: (progress: List<SyncOperationGroup.Progress>) -> Unit = { progress.value = it }

    override suspend fun execute(context: ProcedureExecutionContext) {
        var finishedStepProgress = listOf<SyncOperationGroup.Progress>()

        steps.forEach { step ->
            val confirmed = this.prompter(step)

            if (!confirmed) {
                throw RuntimeException("abandoned")
            }

            val resultProgress =
                step.execute(context, base, dst, logger, progressReport = {
                    progressReport(finishedStepProgress + listOf(it))
                }, limitToSubset)

            finishedStepProgress = finishedStepProgress + listOf(resultProgress)
        }

        progressReport(finishedStepProgress)
    }
}
