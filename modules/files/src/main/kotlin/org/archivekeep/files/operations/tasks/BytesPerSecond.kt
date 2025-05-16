package org.archivekeep.files.operations.tasks

import kotlin.time.Duration

data class BytesPerSecond(
    val value: Long,
) {
    constructor(
        totalBytes: Long,
        duration: Duration,
    ) : this(
        ((totalBytes.toDouble() / (duration.inWholeMilliseconds + 1)) * 1000).toLong(),
    )

    override fun toString(): String =
        if (value < 1024) {
            "$value B/s"
        } else if (value < 1024 * 1024) {
            "${value / 1024} kB/s"
        } else {
            "${value / 1024 / 1024} MB/s"
        }
}
