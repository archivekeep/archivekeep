package org.archivekeep.files.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

fun runBlockingTest(testBody: suspend GenericTestScope.() -> Unit) =
    runBlocking {
        val backgroundScope = CoroutineScope(SupervisorJob())

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
            backgroundScope.cancel()
        }
    }
