package org.archivekeep.app.core.procedures.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.files.procedures.progress.OperationProgress
import java.util.concurrent.CancellationException

abstract class AbstractProcedureJob : UniqueJobGuard.RunnableJob {
    abstract suspend fun execute()

    private var job: Job? = null

    val executionState =
        MutableStateFlow<ProcedureExecutionState>(ProcedureExecutionState.NotStarted)
    val inProgressOperationsStatsMutableFlow =
        MutableStateFlow(emptyList<OperationProgress>())

    override suspend fun run(job: Job) {
        this.job = job

        var resultError: Throwable? = null

        try {
            executionState.value = ProcedureExecutionState.Running

            execute()
        } catch (e: CancellationException) {
            println("Operation job ${javaClass.name} cancelled: $e")
            resultError = e
        } catch (e: Throwable) {
            println("Operation job ${javaClass.name} failed: $e")
            e.printStackTrace()
            resultError = e
        }

        executionState.value = ProcedureExecutionState.Finished(resultError)
    }

    fun cancel() {
        val runningJob = job ?: throw IllegalStateException("Not running")

        runningJob.cancel(message = "Cancelled by user")
        println("Cancelled")
    }
}
