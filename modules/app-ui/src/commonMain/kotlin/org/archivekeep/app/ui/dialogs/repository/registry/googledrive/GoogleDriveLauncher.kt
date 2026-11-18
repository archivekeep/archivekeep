package org.archivekeep.app.ui.dialogs.repository.registry.googledrive

import androidx.compose.runtime.Composable

sealed interface GoogleDriveAuthResult {
    data class Success(
        val credentialsJson: String,
    ) : GoogleDriveAuthResult

    data class Failure(
        val error: Throwable,
    ) : GoogleDriveAuthResult

    data object Cancelled : GoogleDriveAuthResult
}

sealed interface GoogleDriveFolderPickResult {
    data class Success(
        val folderId: String,
        val folderName: String,
    ) : GoogleDriveFolderPickResult

    data class Failure(
        val error: Throwable,
    ) : GoogleDriveFolderPickResult

    data object Cancelled : GoogleDriveFolderPickResult
}

@Composable
expect fun createGoogleDriveAuthLauncher(
    onResult: (GoogleDriveAuthResult) -> Unit,
): (() -> Unit)?

