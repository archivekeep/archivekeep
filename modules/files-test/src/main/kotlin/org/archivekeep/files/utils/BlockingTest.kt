package org.archivekeep.files.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking

fun runBlockingTest(testBody: suspend GenericTestScope.() -> Unit) =
    runBlocking(Job()) {
        val supervisorJob = SupervisorJob(coroutineContext.job)
        val backgroundScope = CoroutineScope(supervisorJob)

        try {
            val currentScope = this@runBlocking

            val scope =
                object : GenericTestScope, CoroutineScope by currentScope {
                    override val backgroundScope = backgroundScope
                }

            with(scope) {
                testBody()
            }
        } finally {
            supervisorJob.cancelAndJoin()
        }
    }
