package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import org.archivekeep.app.ui.domain.wiring.LocalComposeWindow

@Composable
actual fun platformSpecificFileSystemRepositoryGuard(): PlatformSpecificPermissionFulfilment = PlatformSpecificPermissionFulfilment.IsFine

@Composable
actual fun filesystemRepositoryDirectoryPicker(onResult: (result: PickResult) -> Unit): () -> Unit {
    val pickerLauncher =
        rememberDirectoryPickerLauncher(
            title = "Pick a directory",
            platformSettings =
                FileKitPlatformSettings(
                    parentWindow = LocalComposeWindow.current,
                ),
        ) { directory ->
            if (directory == null) {
                onResult(PickResult.Failure(RuntimeException("Directory picker returned null directory")))
            } else {
                val path = directory.path

                if (path == null) {
                    onResult(PickResult.Failure(RuntimeException("Path not present for $directory")))
                } else {
                    onResult(PickResult.Success(path))
                }
            }
        }

    return pickerLauncher::launch
}
