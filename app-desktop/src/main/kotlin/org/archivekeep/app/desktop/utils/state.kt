package org.archivekeep.app.desktop.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshots.StateFactoryMarker
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.app.core.utils.generics.ExecutionOutcome

@Composable
fun (Deferred<ExecutionOutcome>).produceState() =
    produceState<ExecutionOutcome?>(null, this) {
        value = await()
    }

@Composable
fun <T> MutableStateFlow<T>.asMutableState(): MutableState<T> {
    val state = this.collectAsState()

    return object : MutableState<T> {
        override var value: T
            get() = state.value
            set(value) {
                this@asMutableState.value = value
            }

        override fun component1(): T = value

        override fun component2(): (T) -> Unit =
            {
                this.value = it
            }
    }
}

@StateFactoryMarker
fun <T> derivedMutableStateOf(
    onSet: (T) -> Unit,
    calculation: () -> T,
): MutableState<T> {
    val state = derivedStateOf(calculation)

    return object : MutableState<T> {
        override var value: T
            get() = state.value
            set(value) {
                onSet(value)
            }

        override fun component1(): T = value

        override fun component2(): (T) -> Unit =
            {
                this.value = it
            }
    }
}
