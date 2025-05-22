package org.archivekeep.utils.procedures.tasks

import kotlinx.coroutines.flow.StateFlow

interface ProcedureJobTask<in C> {
    val executionProgressSummaryStateFlow: StateFlow<TaskExecutionProgressSummary>

    suspend fun execute(context: C)
}
