package org.archivekeep.app.desktop.ui.dialogs.repositories

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Lock
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.data.canUnlock
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.designsystem.input.CheckboxWithText
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.rememberLaunchableAction
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

class RepositoryUnlockDialog(
    val uri: RepositoryURI,
    val onUnlock: (() -> Unit)? = null,
) : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val repositoryService = LocalRepoService.current
        val walletOperationLaunchers = LocalWalletOperationLaunchers.current
        val credentialStorage = LocalWalletDataStore.current

        val repositoryState = repositoryService.getRepository(repositoryURI = uri)

        val unlockAction = rememberLaunchableAction()

        var basicAuthCredentials by remember {
            mutableStateOf<BasicAuthCredentials?>(null)
        }
        var unlockOptions by remember {
            mutableStateOf(
                UnlockOptions(
                    rememberSession = false,
                ),
            )
        }

        val launchAuthenticate =
            basicAuthCredentials?.let { presentBasicCredentials ->
                {
                    unlockAction.launch {
                        if (unlockOptions.rememberSession) {
                            if (!walletOperationLaunchers.ensureWalletForWrite()) {
                                return@launch
                            }
                        }

                        repositoryState.unlock(
                            presentBasicCredentials,
                            unlockOptions,
                        )
                    }
                }
            }

        DialogOverlayCard(onDismissRequest = onClose) {
            DialogInnerContainer(
                buildAnnotatedString {
                    append("Unlock repository")
                },
                content = {
                    if (repositoryState.needsUnlock.collectAsState(null).value == false) {
                        Text("Unlocked")
                        return@DialogInnerContainer
                    }

                    Text("Authentication is needed to access repository")
                    Spacer(Modifier.height(12.dp))

                    if (credentialStorage.canUnlock()) {
                        HorizontalDivider(Modifier.padding(bottom = 12.dp), thickness = 1.dp)
                        Text(
                            "Wallet with stored credentials is locked.",
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        OutlinedButton(
                            onClick = {
                                walletOperationLaunchers.openUnlockWallet({
                                    onClose()
                                    onUnlock?.let { it() }
                                })
                            },
                        ) {
                            Icon(
                                TablerIcons.Lock,
                                contentDescription = "Locked wallet",
                                Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))

                            Text("Open wallet")
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 1.dp)
                    }

                    Text(
                        "Enter credentials to authenticate with:",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        basicAuthCredentials?.username ?: "",
                        onValueChange = {
                            basicAuthCredentials =
                                BasicAuthCredentials(
                                    username = it,
                                    password = basicAuthCredentials?.password ?: "",
                                )
                        },
                        placeholder = {
                            Text("Enter username ...")
                        },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        basicAuthCredentials?.password ?: "",
                        onValueChange = {
                            basicAuthCredentials =
                                BasicAuthCredentials(
                                    password = it,
                                    username = basicAuthCredentials?.username ?: "",
                                )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = {
                            Text("Enter password ...")
                        },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    CheckboxWithText(
                        unlockOptions.rememberSession,
                        onValueChange = {
                            unlockOptions =
                                unlockOptions.copy(
                                    rememberSession = it,
                                )
                        },
                        text = "Remember session",
                    )
                },
                bottomContent = {
                    DialogButtonContainer {
                        DialogPrimaryButton(
                            "Authenticate",
                            onClick = launchAuthenticate ?: {},
                            enabled =
                                !unlockAction.isRunning &&
                                    launchAuthenticate != null &&
                                    basicAuthCredentials?.let { it.username.isNotBlank() && it.password.isNotBlank() } ?: false,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        DialogDismissButton(
                            "Cancel",
                            onClick = onClose,
                        )
                    }
                },
            )
        }
    }
}
