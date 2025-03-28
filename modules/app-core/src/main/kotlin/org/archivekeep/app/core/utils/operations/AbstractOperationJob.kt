package org.archivekeep.app.core.utils.operations

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.app.core.utils.UniqueJobGuard
import java.util.concurrent.CancellationException

abstract class AbstractOperationJob : UniqueJobGuard.RunnableJob {
    abstract suspend fun execute()

    private var job: Job? = null

    val executionState = MutableStateFlow<OperationExecutionState>(OperationExecutionState.NotStarted)

    override suspend fun run(job: Job) {
        this.job = job

        var resultError: Throwable? = null

        try {
            executionState.value = OperationExecutionState.Running

            execute()
        } catch (e: CancellationException) {
            println("Operation job ${javaClass.name} cancelled: $e")
            resultError = e
        } catch (e: Throwable) {
            println("Operation job ${javaClass.name} failed: $e")
            e.printStackTrace()
            resultError = e
        }

        executionState.value = OperationExecutionState.Finished(resultError)
    }

    fun cancel() {
        val runningJob = job ?: throw IllegalStateException("Not running")

        runningJob.cancel(message = "Cancelled by user")
        println("Cancelled")
    }
}
