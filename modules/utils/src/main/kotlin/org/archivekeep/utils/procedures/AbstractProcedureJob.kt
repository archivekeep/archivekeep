package org.archivekeep.utils.procedures

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.utils.procedures.operations.OperationContext
import org.archivekeep.utils.procedures.operations.OperationProgress
import org.archivekeep.utils.procedures.operations.ProgressTracker
import java.util.concurrent.CancellationException

abstract class AbstractProcedureJob {
    val scope = CoroutineScope(SupervisorJob())

    private val progressTracker = ProgressTracker(scope)

    val inProgressOperationsProgressFlow = progressTracker.inProgressOperationsProgressFlow

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
                        progressTracker.runWithTracker { mainReport ->
                            val operationContext =
                                object : OperationContext {
                                    override fun progressReport(progress: OperationProgress) {
                                        mainReport(progress)
                                    }
                                }

                            block(operationContext)
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
