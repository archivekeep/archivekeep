package org.archivekeep.files.procedures.indexupdate

import kotlin.time.Duration.Companion.nanoseconds

/**
 * Measures the execution time of [block] and tracks it with [tracker] under [operationId].
 */
inline fun <T> RepoPerformanceTracker.measure(
    operationId: String,
    block: () -> T,
): T {
    val start = System.nanoTime()
    val result = block()
    track(operationId, (System.nanoTime() - start).nanoseconds)
    return result
}
