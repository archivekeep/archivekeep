package org.archivekeep.app.ui.components.feature.dialogs.operations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.ui.components.feature.errors.AutomaticErrorMessage
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.utils.procedures.ProcedureExecutionState
import java.util.concurrent.CancellationException

@Composable
fun ExecutionErrorIfPresent(
    executionState: ProcedureExecutionState,
    spacerModifier: Modifier = Modifier.height(12.dp)
) {
    (executionState as? ProcedureExecutionState.Finished)
        ?.error
        ?.let { error ->
            if (error !is CancellationException) {
                Spacer(spacerModifier)
                AutomaticErrorMessage(error, onResolve = {})
            }
        }
}

@Composable
fun ExecutionErrorIfPresent(executionOutcome: ExecutionOutcome?) {
    if ((executionOutcome is ExecutionOutcome.Failed)) {
        if (executionOutcome.cause !is CancellationException) {
            Spacer(Modifier.height(12.dp))
            AutomaticErrorMessage(executionOutcome.cause, onResolve = {})
        }
    }
}

@Composable
fun LaunchableExecutionErrorIfPresent(launchable: Launchable<*>) {
    (launchable.executionOutcome.value as? ExecutionOutcome.Failed)
        ?.let { outcome ->
            if (outcome.cause !is CancellationException) {
                Spacer(Modifier.height(12.dp))
                AutomaticErrorMessage(outcome.cause, onResolve = launchable.reset)
            }
        }
}
