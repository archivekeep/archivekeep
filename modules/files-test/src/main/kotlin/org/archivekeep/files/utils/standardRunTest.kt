package org.archivekeep.files.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

fun standardRunTest(testBody: suspend GenericTestScope.() -> Unit) =
    runTest {
        val scope =
            object : GenericTestScope, CoroutineScope by this@runTest {
                override val backgroundScope = this@runTest.backgroundScope

                override val ioDispatcher = StandardTestDispatcher(this@runTest.testScheduler)
            }

        with(scope) {
            testBody()
        }
    }
