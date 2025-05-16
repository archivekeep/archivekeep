package org.archivekeep.app.core.utils

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

abstract class AbstractJobGuardRunnable : UniqueJobGuard.RunnableJob {
    abstract suspend fun execute()

    private var job: Job? = null

    override suspend fun run(job: Job) {
        this.job = job

        execute()
    }

    fun cancel() {
        val runningJob = job ?: throw IllegalStateException("Not running")

        runningJob.cancel(message = "Cancelled by user")
        println("Cancelled")
    }
}
