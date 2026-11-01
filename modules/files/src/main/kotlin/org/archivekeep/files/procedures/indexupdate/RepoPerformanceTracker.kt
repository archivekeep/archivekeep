package org.archivekeep.files.procedures.indexupdate

import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

sealed interface RepoPerformanceTracker {
    fun track(
        operationId: String,
        duration: Duration,
    )

    fun dump(out: PrintWriter? = null)

    object NoOp : RepoPerformanceTracker {
        override fun track(
            operationId: String,
            duration: Duration,
        ) {}

        override fun dump(out: PrintWriter?) {}
    }

    class DefaultImpl : RepoPerformanceTracker {
        private val stats = ConcurrentHashMap<String, StatEntry>()

        private data class StatEntry(
            val count: AtomicLong = AtomicLong(0),
            val totalNanos: AtomicLong = AtomicLong(0),
        )

        override fun track(
            operationId: String,
            duration: Duration,
        ) {
            val entry = stats.computeIfAbsent(operationId) { StatEntry() }
            entry.count.incrementAndGet()
            entry.totalNanos.addAndGet(duration.inWholeNanoseconds)
        }

        override fun dump(out: PrintWriter?) {
            val output = StringBuilder()

            if (stats.isEmpty()) {
                output.appendLine("No performance stats recorded.")
            } else {
                output.appendLine()
                output.appendLine("=== Repository Performance Stats ===")
                output.appendLine()

                val sortedEntries = stats.entries.sortedBy { it.key }

                for ((operationId, entry) in sortedEntries) {
                    val count = entry.count.get()
                    val totalNanos = entry.totalNanos.get()
                    val total = totalNanos.nanoseconds
                    val avg = if (count > 0) (totalNanos / count).nanoseconds else Duration.ZERO

                    output.appendLine("  $operationId:")
                    output.appendLine("    count:   $count")
                    output.appendLine("    total:   $total")
                    output.appendLine("    average: $avg")
                    output.appendLine()
                }
            }

            print(output)
            out?.print(output)
            out?.flush()
        }
    }
}
