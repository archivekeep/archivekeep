package org.archivekeep.utils.procedures

import org.archivekeep.utils.procedures.tasks.ProcedureJobTask

class TaskProcedureJob<T : ProcedureJobTask<ProcedureExecutionContext>>(
    val task: T,
) : AbstractProcedureJob() {
    override suspend fun execute(context: ProcedureExecutionContext) {
        task.execute(context)
    }
}
