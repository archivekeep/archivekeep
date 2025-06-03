package org.archivekeep.app.ui.utils

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
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

    fun launch(fn: suspend () -> Unit) {
        if (state is State.Running) {
            throw IllegalStateException("Already running")
        }

        scope.launch {
            try {
                state = State.Running(coroutineContext.job)

                fn()

                state = State.Completed(ExecutionOutcome.Success())
            } catch (e: Throwable) {
                println("Execution failed")
                e.printStackTrace()
                state = State.Completed(ExecutionOutcome.Failed(e))
            }
        }
    }

    fun reset() {
        if (state !is State.Completed) {
            throw IllegalStateException("Not completed")
        }

        state = null
    }
}
