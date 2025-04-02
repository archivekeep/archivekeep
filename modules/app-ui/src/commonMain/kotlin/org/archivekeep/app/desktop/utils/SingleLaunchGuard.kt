package org.archivekeep.app.desktop.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.archivekeep.app.core.utils.generics.ExecutionOutcome

class SingleLaunchGuard(
    val scope: CoroutineScope,
) {
    var runningJob by mutableStateOf<Job?>(null)
        private set

    var executionOutcome = mutableStateOf<ExecutionOutcome?>(null)

    fun launch(fn: suspend () -> Unit) {
        runningJob =
            scope.launch {
                try {
                    fn()

                    executionOutcome.value = ExecutionOutcome.Success()
                } catch (e: Throwable) {
                    executionOutcome.value = ExecutionOutcome.Failed(e)
                } finally {
                    runningJob = null
                }
            }
    }

    fun reset() {
        executionOutcome.value = null
    }
}
