package org.archivekeep.utils.procedures.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.utils.safeCombine

open class SequentialProcedureJobTaskGroup<C, IC>(
    val subTasks: List<ProcedureJobTask<IC>>,
    val produceInnerContext: suspend (context: C) -> IC,
) : ProcedureJobTask<C> {
    val scope = CoroutineScope(SupervisorJob())

    override val executionProgressSummaryStateFlow: StateFlow<TaskExecutionProgressSummary.Group> =
        safeCombine(
            subTasks.map { it.executionProgressSummaryStateFlow },
        ) { subProgress ->
            TaskExecutionProgressSummary.Group(subProgress.toList())
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            TaskExecutionProgressSummary.Group(subTasks.map { it.executionProgressSummaryStateFlow.value }),
        )

    override suspend fun execute(context: C) {
        val innerContext = produceInnerContext(context)

        subTasks.forEach { subTask ->
            subTask.execute(innerContext)
        }
    }
}
