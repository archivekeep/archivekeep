package org.archivekeep.app.desktop.ui.components.dialogs.operations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.app.desktop.ui.components.errors.AutomaticErrorMessage
import java.util.concurrent.CancellationException

@Composable
fun ExecutionErrorIfPresent(executionState: OperationExecutionState) {
    (executionState as? OperationExecutionState.Finished)
        ?.error
        ?.let { error ->
            if (error !is CancellationException) {
                Spacer(Modifier.height(12.dp))
                AutomaticErrorMessage(error, onResolve = {})
            }
        }
}
