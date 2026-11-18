package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.ui.dialogs.repository.registry.googledrive.GoogleDriveFolderPickResult
import org.archivekeep.app.ui.dialogs.repository.registry.googledrive.GoogleDriveFolderPicker
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices

@Composable
fun GoogleDriveFolderPickerDialog(
    credentialsJson: String,
    onFolderSelected: (folderId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val appServices = LocalApplicationServices.current
    var authError by remember { mutableStateOf<Throwable?>(null) }

    DialogOverlayCard(onDismissRequest = onDismiss) {
        GoogleDriveFolderPicker(
            credentialsJson = credentialsJson,
            authProvider = appServices.googleDriveAuthProvider,
            onResult = { result ->
                when (result) {
                    is GoogleDriveFolderPickResult.Success -> {
                        onFolderSelected(result.folderId)
                        onDismiss()
                    }

                    is GoogleDriveFolderPickResult.Failure -> {
                        authError = result.error
                        onDismiss()
                    }

                    GoogleDriveFolderPickResult.Cancelled -> {
                        onDismiss()
                    }
                }
            },
        )
    }
}
