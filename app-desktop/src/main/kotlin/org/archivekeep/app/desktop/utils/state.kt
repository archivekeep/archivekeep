package org.archivekeep.app.desktop.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.app.core.utils.generics.ExecutionOutcome

@Composable
fun (Deferred<ExecutionOutcome>).produceState() =
    produceState<ExecutionOutcome?>(null, this) {
        value = await()
    }

@Composable
fun <T> MutableStateFlow<T>.asState(): MutableState<T> {
    val state = this.collectAsState()

    return object : MutableState<T> {
        override var value: T
            get() = state.value
            set(value) {
                this@asState.value = value
            }

        override fun component1(): T = value

        override fun component2(): (T) -> Unit =
            {
                this.value = it
            }
    }
}
