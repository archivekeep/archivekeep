package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import org.archivekeep.files.procedures.progress.BytesPerSecond
import org.archivekeep.files.procedures.progress.CopyOperationProgress
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import org.archivekeep.utils.procedures.operations.OperationContext
import org.archivekeep.utils.procedures.operations.OperationProgress
import org.archivekeep.utils.procedures.operations.ProgressTracker
import kotlin.time.Duration.Companion.seconds

internal class SyncProgressTracingSubContext(
    flowScope: CoroutineScope,
    private val parentProcedureContext: ProcedureExecutionContext,
) : ProcedureExecutionContext {
    private val subTracker = ProgressTracker(flowScope)

    private val historicalProgress = MutableStateFlow(emptyList<OperationProgress>())
    private val inProgressOperationsProgressFlow = subTracker.inProgressOperationsProgressFlow

    val speedFlow =
        combine(
            historicalProgress,
            inProgressOperationsProgressFlow,
        ) { a, b ->
            Pair(a, b)
        }.conflate()
            .transform {
                emit(it)

                if (it.first.isNotEmpty() && it.second.isNotEmpty()) {
                    delay(1.seconds)
                }
            }.map { (a, b) ->
                val copyOperations =
                    (a + b).filterIsInstance<CopyOperationProgress>()

                val (weightSum, valueSum) =
                    copyOperations.foldIndexed(
                        Pair(0L, 0L),
                    ) { index, acc, it ->
                        if (it.velocity != null) {
                            val (weightSum, valueSum) = acc

                            Pair(weightSum + (index + 1), valueSum + (index + 1) * it.velocity.value)
                        } else {
                            acc
                        }
                    }

                if (weightSum > 0) {
                    BytesPerSecond(valueSum / weightSum)
                } else {
                    null
                }
            }

    override suspend fun runOperation(block: suspend (operationContext: OperationContext) -> Unit) {
        parentProcedureContext.runOperation { parentOperationContext ->
            var lastProgress: OperationProgress? = null

            subTracker.runWithTracker { subReport ->
                val operationContext =
                    object : OperationContext by parentOperationContext {
                        override fun progressReport(progress: OperationProgress) {
                            parentOperationContext.progressReport(progress)
                            subReport(progress)
                            lastProgress = progress
                        }
                    }

                block(operationContext)
            }

            lastProgress?.let { lastProgress ->
                historicalProgress.update {
                    it + listOf(lastProgress)
                }
            }
        }
    }
}
