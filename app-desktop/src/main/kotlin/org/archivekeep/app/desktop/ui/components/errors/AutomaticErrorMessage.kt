package org.archivekeep.app.desktop.ui.components.errors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.utils.exceptions.RepositoryLockedException
import org.archivekeep.app.core.utils.generics.ExecutionOutcome

@Composable
fun AutomaticErrorMessage(
    error: ExecutionOutcome.Failed,
    onResolve: () -> Unit,
) {
    AutomaticErrorMessage(error, onResolve)
}

@Composable
fun AutomaticErrorMessage(
    cause: Throwable,
    onResolve: () -> Unit,
) {
    Surface(
        border =
            BorderStroke(
                width = 1.dp,
                color = Color.Red,
            ),
    ) {
        Column(
            Modifier.padding(12.dp),
        ) {
            if (cause is RepositoryLockedException) {
                RepositoryLockedError(cause, onResolve)
            } else {
                GeneralErrorMessage(cause)
            }
        }
    }
}
