package org.archivekeep.app.ui.utils

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.archivekeep.app.core.utils.generics.ExecutionOutcome

class SingleLaunchGuard(
    val scope: CoroutineScope,
) {
    sealed interface State {
        data class Running(
            val job: Job,
        ) : State

        data class Completed(
            val outcome: ExecutionOutcome,
        ) : State
    }

    var state by mutableStateOf<State?>(null)
        private set

    val runningJob by derivedStateOf { (state as? State.Running)?.job }
    val executionOutcome = derivedStateOf { (state as? State.Completed)?.outcome }

    @Synchronized
    fun launch(fn: suspend () -> Unit) {
        if (state is State.Running) {
            throw IllegalStateException("Already running")
        }

        val job =
            scope.launch {
                try {
                    withContext(Dispatchers.Default) {
                        fn()
                    }

                    state = State.Completed(ExecutionOutcome.Success())
                } catch (e: Throwable) {
                    println("Execution failed")
                    e.printStackTrace()
                    state = State.Completed(ExecutionOutcome.Failed(e))
                }
            }
        state = State.Running(job)
    }

    @Synchronized
    fun reset() {
        if (state !is State.Completed) {
            throw IllegalStateException("Not completed")
        }

        state = null
    }
}
