package org.archivekeep.files.procedures.sync.discovery

import org.archivekeep.files.procedures.sync.job.SyncProcedureJobTask
import org.archivekeep.files.procedures.sync.log.SyncLogger
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.TaskProcedureJob
import org.archivekeep.utils.procedures.tasks.SequentialProcedureJobTaskGroup

class DiscoveredSync internal constructor(
    val groups: List<DiscoveredSyncOperationsGroup<*>>,
) {
    fun createJob(
        base: Repo,
        dst: Repo,
        // TODO: this should work against real tasks (filtered by limitToSubset)
        prompter: suspend (step: DiscoveredSyncOperationsGroup<*>) -> Boolean,
        logger: SyncLogger,
        limitToSubset: Set<SyncOperation>? = null,
    ) = TaskProcedureJob(
        SequentialProcedureJobTaskGroup(
            subTasks = groups.map { it.createJobTask(limitToSubset) },
            produceInnerContext =
                { context ->
                    SyncProcedureJobTask.Context(
                        context,
                        base,
                        dst,
                        logger,
                        prompter,
                    )
                },
        ),
    )

    fun isNoOp(): Boolean = groups.isEmpty() || groups.all { it.isNoOp() }
}
