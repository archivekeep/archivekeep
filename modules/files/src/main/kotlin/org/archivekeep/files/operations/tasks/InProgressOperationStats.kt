package org.archivekeep.files.operations.tasks

import kotlin.time.Duration

interface InProgressOperationStats {
    val timeConsumed: Duration
    val timeEstimated: Duration?
    val velocity: Any
    val completion: Float
}
