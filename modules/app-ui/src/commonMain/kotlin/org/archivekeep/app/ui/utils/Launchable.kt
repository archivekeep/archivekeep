package org.archivekeep.app.ui.utils

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.utils.generics.ExecutionOutcome

data class Launchable<P>(
    val onLaunch: (p: P) -> Unit,
    val isRunning: State<Boolean>,
    val executionOutcome: State<ExecutionOutcome?>,
    val reset: () -> Unit = {},
)

fun simpleLaunchable(
    coroutineScope: CoroutineScope,
    fn: suspend () -> Unit,
) = simpleLaunchable<Unit>(coroutineScope, fn = { fn() })

fun <P> simpleLaunchable(
    coroutineScope: CoroutineScope,
    fn: suspend (P) -> Unit,
): Launchable<P> =
    SingleLaunchGuard(coroutineScope).let { singleLaunchGuard ->
        Launchable(
            onLaunch = { singleLaunchGuard.launch { fn(it) } },
            isRunning = derivedStateOf { singleLaunchGuard.runningJob != null },
            executionOutcome = singleLaunchGuard.executionOutcome,
            reset = singleLaunchGuard::reset,
        )
    }

fun Launchable<Unit>.asTrivialAction() =
    derivedStateOf {
        ActionTriggerState(
            onLaunch = { with(this) { onLaunch(Unit) } },
            canLaunch = true,
            isRunning = isRunning.value,
        )
    }

inline fun <P> Launchable<P>.asAction(
    crossinline onLaunch: Launchable<P>.() -> Unit,
    crossinline canLaunch: () -> Boolean = { true },
) = derivedStateOf {
    ActionTriggerState(
        onLaunch = { with(this) { onLaunch() } },
        canLaunch = canLaunch(),
        isRunning = isRunning.value,
    )
}

fun <P> mockLaunchable(
    isRunning: Boolean,
    executionOutcome: ExecutionOutcome?,
): Launchable<P> = Launchable({}, mutableStateOf(isRunning), mutableStateOf(executionOutcome))
