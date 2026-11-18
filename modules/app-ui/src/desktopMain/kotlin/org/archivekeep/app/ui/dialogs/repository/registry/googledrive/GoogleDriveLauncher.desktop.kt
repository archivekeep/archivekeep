package org.archivekeep.app.ui.dialogs.repository.registry.googledrive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices

@Composable
actual fun createGoogleDriveAuthLauncher(
    onResult: (GoogleDriveAuthResult) -> Unit,
): (() -> Unit)? {
    val appServices = LocalApplicationServices.current
    val provider = appServices.googleDriveAuthProvider
    val coroutineScope = rememberCoroutineScope()

    if (!provider.isSupported) {
        return null
    }

    return remember {
        {
            coroutineScope.launch {
                try {
                    val credentialsJson = provider.authenticate()
                    onResult(GoogleDriveAuthResult.Success(credentialsJson))
                } catch (e: Throwable) {
                    onResult(GoogleDriveAuthResult.Failure(e))
                }
            }
        }
    }
}
