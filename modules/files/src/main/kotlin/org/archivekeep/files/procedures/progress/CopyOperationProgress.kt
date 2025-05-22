package org.archivekeep.files.procedures.progress

import org.archivekeep.utils.procedures.operations.OperationProgress
import kotlin.time.Duration

data class CopyOperationProgress(
    val filename: String,
    override val timeConsumed: Duration?,
    val copied: Long,
    val total: Long,
) : OperationProgress {
    override val timeEstimated: Duration? =
        if (total > 0 && copied > 0 && timeConsumed != null) {
            timeConsumed * ((total - copied).toDouble() / copied)
        } else {
            null
        }

    override val velocity: BytesPerSecond? = if (timeConsumed != null) BytesPerSecond(copied, timeConsumed) else null

    override val completion: Float
        // TODO: handle unknown total
        get() = if (total > 0) copied.toFloat() / total else 0f
}
