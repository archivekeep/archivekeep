package org.archivekeep.app.ui.dialogs.repository.registry.googledrive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archivekeep.app.core.persistence.drivers.googledrive.GoogleDriveAuthProvider
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumn
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.elements.ErrorAlert
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.errors.GeneralErrorMessage
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.googledrive_drive_root
import org.archivekeep.ui.resources.googledrive_go_back
import org.archivekeep.ui.resources.googledrive_loading_folders
import org.archivekeep.ui.resources.googledrive_no_folders_found
import org.archivekeep.ui.resources.googledrive_retry
import org.archivekeep.ui.resources.googledrive_select_folder_title
import org.archivekeep.ui.resources.googledrive_select_this_folder
import org.jetbrains.compose.resources.stringResource

data class GoogleDriveFolderItem(
    val id: String,
    val name: String,
)

@Composable
fun GoogleDriveFolderPicker(
    credentialsJson: String,
    onResult: (GoogleDriveFolderPickResult) -> Unit,
    authProvider: GoogleDriveAuthProvider,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    val folders = remember { mutableStateListOf<GoogleDriveFolderItem>() }
    var currentFolderId by remember { mutableStateOf<String?>(null) }
    val breadcrumb = remember { mutableStateListOf<GoogleDriveFolderItem>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(credentialsJson, currentFolderId, authProvider) {
        loading = true
        error = null
        folders.clear()
        val targetFolder = currentFolderId ?: "root"
        try {
            val list = listDriveFolders(credentialsJson, targetFolder, authProvider)
            folders.addAll(list)
        } catch (e: Throwable) {
            e.printStackTrace()
            error = e
        } finally {
            loading = false
        }
    }

    DialogInnerContainer(
        title = buildAnnotatedString { append(stringResource(Res.string.googledrive_select_folder_title)) },
        content = {
            if (breadcrumb.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = {
                        breadcrumb.clear()
                        currentFolderId = null
                    }) {
                        Text(stringResource(Res.string.googledrive_drive_root))
                    }
                    breadcrumb.forEachIndexed { index, folder ->
                        Text(" / ")
                        TextButton(onClick = {
                            breadcrumb.removeLast()
                            while (breadcrumb.size > index) {
                                breadcrumb.removeLast()
                            }
                            currentFolderId = folder.id
                        }) {
                            Text(folder.name)
                        }
                    }
                }
            }

            when {
                loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(20.dp))
                        CircularProgressIndicator()
                        Spacer(Modifier.height(20.dp))
                        Text(stringResource(Res.string.googledrive_loading_folders))
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(20.dp))
                        ErrorAlert {
                            GeneralErrorMessage(error!!)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            currentFolderId = if (breadcrumb.isEmpty()) null else breadcrumb.last().id
                            if (breadcrumb.isNotEmpty()) {
                                breadcrumb.removeLast()
                            }
                        }) {
                            if (breadcrumb.isNotEmpty()) {
                                Text(stringResource(Res.string.googledrive_go_back))
                            } else {
                                Text(stringResource(Res.string.googledrive_retry))
                            }
                        }
                    }
                }

                folders.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(20.dp))
                        Text(stringResource(Res.string.googledrive_no_folders_found))
                        Spacer(Modifier.height(8.dp))
                        if (breadcrumb.isNotEmpty()) {
                            TextButton(onClick = {
                                breadcrumb.removeLast()
                                currentFolderId = if (breadcrumb.isEmpty()) null else breadcrumb.last().id
                            }) {
                                Text(stringResource(Res.string.googledrive_go_back))
                            }
                        }
                    }
                }

                else -> {
                    ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(folders, key = { it.id }) { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        breadcrumb.add(folder)
                                        currentFolderId = folder.id
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(folder.name)
                            }
                        }
                    }
                }
            }

        },

        bottomContent = {
            DialogButtonContainer {
                SimpleActionDialogControlButtons(
                    launchText = stringResource(Res.string.googledrive_select_this_folder),
                    onLaunch = {
                        val displayName = if (breadcrumb.isEmpty()) "Root" else breadcrumb.last().name
                        onResult(
                            GoogleDriveFolderPickResult.Success(
                                folderId = currentFolderId ?: "root",
                                folderName = displayName,
                            )
                        )
                    },
                    onClose = { onResult(GoogleDriveFolderPickResult.Cancelled) },
                    canLaunch = !loading && error == null
                )
            }
        })
}

suspend fun listDriveFolders(
    credentialsJson: String,
    parentFolderId: String,
    authProvider: GoogleDriveAuthProvider,
): List<GoogleDriveFolderItem> =
    withContext(Dispatchers.IO) {
        val drive = authProvider.createDriveClient(credentialsJson)
        val result = mutableListOf<GoogleDriveFolderItem>()
        var pageToken: String? = null

        do {
            val request =
                drive
                    .files()
                    .list()
                    .setQ(
                        "'${parentFolderId.escapeQueryValue()}' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
                    )
                    .setCorpora("allDrives")
                    .setPageSize(100)
                    .setSupportsAllDrives(true)
                    .setIncludeItemsFromAllDrives(true)
                    .setFields("nextPageToken, files(id, name)")

            if (pageToken != null) {
                request.setPageToken(pageToken)
            }

            val response = request.execute()
            response.files.orEmpty().forEach { file ->
                file.id?.let { id ->
                    file.name?.let { name ->
                        result.add(GoogleDriveFolderItem(id, name))
                    }
                }
            }
            pageToken = response.nextPageToken
        } while (pageToken != null)

        result.sortedBy { it.name.lowercase() }
    }

private fun String.escapeQueryValue() = replace("\\", "\\\\").replace("'", "\\'")
