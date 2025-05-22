package org.archivekeep.utils.procedures.tasks

import kotlin.time.Duration

sealed interface TaskExecutionProgressSummary {
    interface Simple : TaskExecutionProgressSummary {
        val timeEstimated: Duration?
        val completion: Float
    }

    data class Group(
        val subTasks: List<TaskExecutionProgressSummary>,
    ) : TaskExecutionProgressSummary
}
