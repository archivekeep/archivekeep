package org.archivekeep.files.performance

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val statsSnapshot: StateFlow<Map<String, OperationStats>>

    object NoOp : RepoPerformanceTracker {
        override val statsSnapshot: StateFlow<Map<String, OperationStats>> = MutableStateFlow(emptyMap())

        override fun track(
            operationId: String,
            duration: Duration,
        ) {
            // not tracking anything
        }

        override fun dump(out: PrintWriter?) {
            // nothing tracked, nothing to dump
        }
    }

    class DefaultImpl : RepoPerformanceTracker {
        private val _statsSnapshot = MutableStateFlow(emptyMap<String, OperationStats>())

        override val statsSnapshot: StateFlow<Map<String, OperationStats>> = _statsSnapshot.asStateFlow()
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
            _statsSnapshot.value = buildSnapshot()
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

        private fun buildSnapshot(): Map<String, OperationStats> {
            val sortedEntries = stats.entries.sortedBy { it.key }
            return sortedEntries.associate { (operationId, entry) ->
                val count = entry.count.get()
                val totalNanos = entry.totalNanos.get()
                val total = totalNanos.nanoseconds
                val avg = if (count > 0) (totalNanos / count).nanoseconds else Duration.ZERO
                operationId to OperationStats(operationId, count, total, avg)
            }
        }
    }
}
