package org.archivekeep.app.ui.components.feature.errors

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.operations.RequiresCredentialsException
import org.archivekeep.app.core.utils.exceptions.RepositoryLockedException
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.ui.components.designsystem.elements.ErrorAlert
import org.archivekeep.utils.exceptions.IncorrectPasswordException

@Composable
fun AutomaticErrorMessage(
    error: ExecutionOutcome.Failed,
    onResolve: () -> Unit,
) {
    AutomaticErrorMessage(error.cause, onResolve)
}

@Composable
fun AutomaticErrorMessage(
    cause: Throwable,
    onResolve: () -> Unit,
) {
    LaunchedEffect(cause) {
        println("Error was shown in UI: ${cause.message}")
        cause.printStackTrace()
    }

    ErrorAlert {
        Column(
            Modifier.padding(12.dp),
        ) {
            when (cause) {
                is RepositoryLockedException -> RepositoryLockedError(cause, onResolve)
                is RequiresCredentialsException -> Text("Credentials are required to access repository, or repository doesn't exist.")
                is IncorrectPasswordException -> Text("Entered password isn't correct.")
                else -> GeneralErrorMessage(cause)
            }
        }
    }
}
