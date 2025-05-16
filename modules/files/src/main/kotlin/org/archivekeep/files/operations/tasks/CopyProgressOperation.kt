package org.archivekeep.files.operations.tasks

import kotlin.time.Duration

data class CopyProgressOperation(
    val filename: String,
    override val timeConsumed: Duration,
    val copied: Long,
    val total: Long,
) : InProgressOperationStats {
    override val timeEstimated: Duration? =
        if (total > 0) {
            timeConsumed * ((total - copied).toDouble() / copied)
        } else {
            null
        }

    override val velocity: BytesPerSecond = BytesPerSecond(copied, timeConsumed)

    override val completion: Float
        // TODO: handle unknown total
        get() = if (total > 0) copied.toFloat() / total else 0f
}
