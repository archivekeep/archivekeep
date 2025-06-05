package org.archivekeep.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.archivekeep.app.ui.utils.filesystem.PickResult

fun SemanticsNodeInteraction.performClickTextInput(text: String) {
    performClick()
    performTextInput(text)
}

fun fixedFilesystemDirectoryPicker(path: String): (@Composable (onResult: (result: PickResult) -> Unit) -> () -> Unit) =
    { onResult ->
        {
            onResult(
                PickResult.Success(
                    path,
                ),
            )
        }
    }
