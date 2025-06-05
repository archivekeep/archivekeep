package org.archivekeep.app.ui.utils.filesystem

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.ui.dialogs.repository.registry.getPathFromURI
import org.archivekeep.app.ui.domain.wiring.LocalFileStores

actual fun defaultFilesystemDirectoryPicker(): @Composable (onResult: (result: PickResult) -> Unit) -> () -> Unit =
    { onResult ->
        val fileStores = LocalFileStores.current

        val pickerLauncher =
            rememberDirectoryPickerLauncher(
                title = "Pick a directory",
                platformSettings = FileKitPlatformSettings(),
            ) { directory ->
                if (directory == null) {
                    onResult(PickResult.Failure(RuntimeException("Directory picker returned null directory")))
                } else {
                    val path = directory.path

                    if (path == null) {
                        onResult(PickResult.Failure(RuntimeException("Path not present for $directory")))
                    } else {
                        onResult(
                            PickResult.Success(
                                getPathFromURI(
                                    // TODO - refactor to be executed in appropriate suspend execution context
                                    runBlocking { fileStores.loadFreshMountPoints() },
                                    directory.uri,
                                ),
                            ),
                        )
                    }
                }
            }

        pickerLauncher::launch
    }
