package org.archivekeep.app.ui.utils.filesystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

sealed interface PickResult {
    data class Success(
        val path: String,
    ) : PickResult

    data class Failure(
        val error: Throwable,
    ) : PickResult
}

val LocalFilesystemDirectoryPicker = staticCompositionLocalOf { defaultFilesystemDirectoryPicker() }

expect fun defaultFilesystemDirectoryPicker(): (@Composable (onResult: (result: PickResult) -> Unit) -> () -> Unit)
