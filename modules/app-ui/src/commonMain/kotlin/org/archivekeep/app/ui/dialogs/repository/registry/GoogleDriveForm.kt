package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.archivekeep.app.ui.components.designsystem.elements.InfoAlert
import org.archivekeep.app.ui.components.designsystem.input.TextField
import org.archivekeep.app.ui.components.feature.errors.AutomaticErrorMessage
import org.archivekeep.app.ui.dialogs.repository.registry.googledrive.GoogleDriveAuthResult
import org.archivekeep.app.ui.dialogs.repository.registry.googledrive.createGoogleDriveAuthLauncher
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.WalletOperationLaunchers
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_remember_credentials
import org.archivekeep.ui.resources.dialog_unlock_wallet_open
import org.archivekeep.ui.resources.googledrive_back_to_sign_in
import org.archivekeep.ui.resources.googledrive_credential_json
import org.archivekeep.ui.resources.googledrive_credential_json_info
import org.archivekeep.ui.resources.googledrive_credential_json_placeholder
import org.archivekeep.ui.resources.googledrive_dont_save_credentials
import org.archivekeep.ui.resources.googledrive_folder_and_auth
import org.archivekeep.ui.resources.googledrive_pick_folder
import org.archivekeep.ui.resources.googledrive_root_folder_id
import org.archivekeep.ui.resources.googledrive_root_folder_id_placeholder
import org.archivekeep.ui.resources.googledrive_share_folder_info
import org.archivekeep.ui.resources.googledrive_sign_in
import org.archivekeep.ui.resources.googledrive_sign_in_unavailable
import org.archivekeep.ui.resources.googledrive_signing_in
import org.archivekeep.ui.resources.googledrive_use_credentials_file
import org.archivekeep.ui.resources.googledrive_waiting_for_browser
import org.archivekeep.ui.resources.googledrive_wallet_unlock_needed
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.jetbrains.compose.resources.stringResource

@Composable
fun GoogleDriveForm(
    input: AddRemoteRepositoryDialog.Input.GoogleDrive,
    isEditable: Boolean,
    onOAuthSignIn: (suspend () -> String)?,
    walletOperationLaunchers: WalletOperationLaunchers,
) {
    var showManualCredentials by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<Throwable?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val appServices = LocalApplicationServices.current

    val launcher = createGoogleDriveAuthLauncher { result ->
        when (result) {
            is GoogleDriveAuthResult.Success -> {
                input.credentialsJson.value = result.credentialsJson
                isSigningIn = false
                showFolderPicker = true
            }

            is GoogleDriveAuthResult.Failure -> {
                authError = result.error
                isSigningIn = false
            }

            GoogleDriveAuthResult.Cancelled -> {
                isSigningIn = false
            }
        }
    }
    val hasAuth = launcher != null || onOAuthSignIn != null

    if (hasAuth && !showManualCredentials && !input.rememberCredentials.value) {
        input.rememberCredentials.value = true
    }

    Text(stringResource(Res.string.googledrive_folder_and_auth))
    Spacer(Modifier.height(4.dp))

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            input.rootFolderId.value,
            onValueChange = { input.rootFolderId.value = it },
            label = { Text(stringResource(Res.string.googledrive_root_folder_id)) },
            placeholder = { Text(stringResource(Res.string.googledrive_root_folder_id_placeholder)) },
            singleLine = true,
            enabled = isEditable,
            modifier = Modifier.fillMaxWidth(),
        )
        if (input.credentialsJson.value.isNotBlank() && isEditable) {
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { showFolderPicker = true },
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.googledrive_pick_folder))
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    if (showFolderPicker) {
        GoogleDriveFolderPickerDialog(
            credentialsJson = input.credentialsJson.value,
            onFolderSelected = { folderId ->
                input.rootFolderId.value = folderId
            },
            onDismiss = { showFolderPicker = false },
        )
    } else if (hasAuth && !showManualCredentials) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (launcher != null) {
                Button(
                    onClick = {
                        isSigningIn = true
                        authError = null
                        launcher()
                    },
                    enabled = isEditable && !isSigningIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(Res.string.googledrive_signing_in))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(Res.string.googledrive_sign_in))
                    }
                }
            } else if (onOAuthSignIn != null) {
                Button(
                    onClick = {
                        isSigningIn = true
                        authError = null
                        coroutineScope.launch {
                            try {
                                val credentialsJson = onOAuthSignIn()
                                input.credentialsJson.value = credentialsJson
                            } catch (e: Throwable) {
                                authError = e
                            } finally {
                                isSigningIn = false
                            }
                        }
                    },
                    enabled = isEditable && !isSigningIn,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(Res.string.googledrive_waiting_for_browser))
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(Res.string.googledrive_sign_in))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { showManualCredentials = true },
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.googledrive_use_credentials_file))
            }
            if (authError != null) {
                Spacer(Modifier.height(8.dp))
                AutomaticErrorMessage(
                    cause = authError!!,
                    onResolve = { authError = null },
                )
            }

            Spacer(Modifier.height(8.dp))
            GoogleDriveWalletAlert(
                walletDataStore = appServices.walletDataStore,
                walletOperationLaunchers = walletOperationLaunchers,
                rememberCredentials = input.rememberCredentials,
                isEditable = isEditable,
            )
        }
    } else if (!hasAuth) {
        Text(
            stringResource(Res.string.googledrive_sign_in_unavailable),
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }

    if (showManualCredentials || !hasAuth) {
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            TextField(
                input.credentialsJson.value,
                onValueChange = { input.credentialsJson.value = it },
                label = { Text(stringResource(Res.string.googledrive_credential_json)) },
                placeholder = { Text(stringResource(Res.string.googledrive_credential_json_placeholder)) },
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(Res.string.googledrive_credential_json_info))
            Text(stringResource(Res.string.googledrive_share_folder_info))
            if (showManualCredentials && hasAuth) {
                TextButton(
                    onClick = { showManualCredentials = false },
                    enabled = isEditable,
                ) {
                    Text(stringResource(Res.string.googledrive_back_to_sign_in))
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText(
        input.rememberCredentials.value,
        onValueChange = { input.rememberCredentials.value = it },
        text = stringResource(Res.string.common_remember_credentials),
    )

    Spacer(Modifier.height(12.dp))
}

@Composable
private fun GoogleDriveWalletAlert(
    walletDataStore: ProtectedDataStore<WalletPO>,
    walletOperationLaunchers: WalletOperationLaunchers,
    rememberCredentials: androidx.compose.runtime.MutableState<Boolean>,
    isEditable: Boolean,
) {
    if (!rememberCredentials.value) return

    val walletData = walletDataStore.data.collectAsState(initial = null).value

    if (walletData is OptionalLoadable.NotAvailable) {
        InfoAlert {
            Text(stringResource(Res.string.googledrive_wallet_unlock_needed))
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    walletOperationLaunchers.openUnlockWallet(null)
                },
                enabled = isEditable,
            ) {
                Text(stringResource(Res.string.dialog_unlock_wallet_open))
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = {
                    rememberCredentials.value = false
                },
                enabled = isEditable,
            ) {
                Text(stringResource(Res.string.googledrive_dont_save_credentials))
            }
        }
    }
}

private typealias WalletPO = org.archivekeep.app.core.persistence.credentials.WalletPO
