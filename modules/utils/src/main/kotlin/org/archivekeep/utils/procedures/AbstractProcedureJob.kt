package org.archivekeep.utils.procedures

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.concurrent.CancellationException

private class Key

abstract class AbstractProcedureJob {
    val scope = CoroutineScope(SupervisorJob())

    private val inProgressOperationsStatsMutable =
        MutableStateFlow(emptyMap<Key, OperationProgress>())

    val inProgressOperationsProgressFlow =
        inProgressOperationsStatsMutable
            .map { it.values.toList() }
            .stateIn(scope, SharingStarted.Lazily, emptyList())

    val executionState =
        MutableStateFlow<ProcedureExecutionState>(ProcedureExecutionState.NotStarted)

    protected abstract suspend fun execute(context: ProcedureExecutionContext)

    suspend fun run() {
        var resultError: Throwable? = null

        try {
            executionState.value = ProcedureExecutionState.Running

            val context =
                object : ProcedureExecutionContext {
                    override suspend fun runOperation(block: suspend (operationContext: OperationContext) -> Unit) {
                        val key = Key()
                        try {
                            val operationContext =
                                object : OperationContext {
                                    override fun progressReport(progress: OperationProgress) {
                                        inProgressOperationsStatsMutable.update {
                                            it.toMutableMap().apply { set(key, progress) }.toMap()
                                        }
                                    }
                                }

                            block(operationContext)
                        } finally {
                            inProgressOperationsStatsMutable.update {
                                it.toMutableMap().apply { remove(key) }.toMap()
                            }
                        }
                    }
                }

            execute(context)
        } catch (e: CancellationException) {
            println("Operation job ${javaClass.name} cancelled: $e")
            resultError = e
        } catch (e: Throwable) {
            println("Operation job ${javaClass.name} failed: $e")
            e.printStackTrace()
            resultError = e
        } finally {
            executionState.value = ProcedureExecutionState.Finished(resultError)
        }
    }
}
