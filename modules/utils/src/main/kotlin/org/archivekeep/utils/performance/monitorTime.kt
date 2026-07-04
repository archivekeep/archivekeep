package org.archivekeep.utils.performance

import kotlin.time.TimeSource.Monotonic.markNow

suspend inline fun <R> monitorTime(
    name: String,
    crossinline block: suspend () -> R,
): R {
    val mark = markNow()
    val result = block()

    println("$name took ${mark.elapsedNow().inWholeMilliseconds}ms")

    return result
}
