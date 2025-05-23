package org.archivekeep.utils.procedures.operations

import kotlin.time.Duration

interface OperationProgress {
    val timeConsumed: Duration?
    val timeEstimated: Duration?
    val velocity: Any?
    val completion: Float
}
