package org.archivekeep.app.desktop.ui.dialogs.repository.access

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Lock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.data.canUnlockFlow
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.WalletOperationLaunchers
import org.archivekeep.app.desktop.ui.components.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.desktop.ui.components.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.input.CheckboxWithText
import org.archivekeep.app.desktop.ui.designsystem.input.PasswordField
import org.archivekeep.app.desktop.ui.designsystem.input.TextField
import org.archivekeep.app.desktop.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.utils.Launchable
import org.archivekeep.app.desktop.utils.asAction
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.app.desktop.utils.mockLaunchable
import org.archivekeep.app.desktop.utils.simpleLaunchable
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.Loadable

class RepositoryUnlockDialog(
    uri: RepositoryURI,
    val onUnlock: (() -> Unit)? = null,
) : AbstractRepositoryDialog<RepositoryUnlockDialog.State, RepositoryUnlockDialog.VM>(uri) {
    data class State(
        val repositoryInfo: RepositoryInformation,
        val needsUnlock: Boolean,
        val canUnlockCredentials: Boolean,
        val launchable: Launchable<Unit>,
        val basicAuthCredentialsState: MutableState<BasicAuthCredentials?>,
        val unlockOptionsState: MutableState<UnlockOptions>,
        val onClose: () -> Unit,
    ) : IState {
        var basicAuthCredentials by basicAuthCredentialsState
        var unlockOptions by unlockOptionsState

        val action by launchable.asAction(
            onLaunch = { onLaunch(Unit) },
            canLaunch = { basicAuthCredentials?.let { it.username.isNotBlank() && it.password.isNotBlank() } ?: false },
        )

        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Unlock repository")
            }
    }

    inner class VM(
        val coroutineScope: CoroutineScope,
        val repository: Repository,
        val walletOperationLaunchers: WalletOperationLaunchers,
        val credentialStorage: JoseStorage<Credentials>,
        val _onClose: () -> Unit,
    ) : IVM {
        val basicAuthCredentialsState = mutableStateOf<BasicAuthCredentials?>(null)
        var basicAuthCredentials by basicAuthCredentialsState

        val unlockOptionsState = mutableStateOf(UnlockOptions(rememberSession = false))
        var unlockOptions by unlockOptionsState

        val unlockAction =
            simpleLaunchable(coroutineScope) { _: Unit ->
                basicAuthCredentials?.let { presentBasicCredentials ->
                    if (unlockOptions.rememberSession) {
                        if (!walletOperationLaunchers.ensureWalletForWrite()) {
                            return@simpleLaunchable
                        }
                    }

                    repository.unlock(
                        presentBasicCredentials,
                        unlockOptions,
                    )
                }
            }

        override fun onClose() {
            _onClose()
        }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): VM {
        val walletOperationLaunchers = LocalWalletOperationLaunchers.current
        val credentialStorage = LocalWalletDataStore.current

        return remember {
            VM(
                scope,
                repository,
                walletOperationLaunchers,
                credentialStorage,
                onClose,
            )
        }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember(vm) {
            combine(
                vm.repository.resolvedState,
                vm.repository.needsUnlock,
                vm.credentialStorage.canUnlockFlow(),
            ) { resolvedState, needsUnlock, canUnlockCredentials ->
                State(
                    repositoryInfo = resolvedState.information,
                    needsUnlock = needsUnlock,
                    canUnlockCredentials = canUnlockCredentials,
                    launchable = vm.unlockAction,
                    basicAuthCredentialsState = vm.basicAuthCredentialsState,
                    unlockOptionsState = vm.unlockOptionsState,
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        if (!state.needsUnlock) {
            Text("Unlocked")
            return
        }
        val walletOperationLaunchers = LocalWalletOperationLaunchers.current

        Text(
            buildAnnotatedString {
                append("Authentication is needed to access ")
                appendBoldSpan(state.repositoryInfo.displayName)
                append(" repository.")
            },
        )
        Spacer(Modifier.height(12.dp))

        if (state.canUnlockCredentials) {
            HorizontalDivider(Modifier.padding(bottom = 12.dp), thickness = 1.dp)
            Text(
                "Wallet with stored credentials is locked.",
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedButton(
                onClick = {
                    walletOperationLaunchers.openUnlockWallet({
                        state.onClose()
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
        TextField(
            state.basicAuthCredentials?.username ?: "",
            onValueChange = {
                state.basicAuthCredentials =
                    BasicAuthCredentials(
                        username = it,
                        password = state.basicAuthCredentials?.password ?: "",
                    )
            },
            placeholder = {
                Text("Enter username ...")
            },
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        PasswordField(
            state.basicAuthCredentials?.password ?: "",
            onValueChange = {
                state.basicAuthCredentials =
                    BasicAuthCredentials(
                        password = it,
                        username = state.basicAuthCredentials?.username ?: "",
                    )
            },
            placeholder = {
                Text("Enter password ...")
            },
        )
        Spacer(Modifier.height(12.dp))
        CheckboxWithText(
            state.unlockOptions.rememberSession,
            onValueChange = {
                state.unlockOptions =
                    state.unlockOptions.copy(
                        rememberSession = it,
                    )
            },
            text = "Remember session",
        )
        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        SimpleActionDialogControlButtons(
            "Authenticate",
            actionState = state.action,
            onClose = state.onClose,
        )
    }
}

@Composable
@Preview
private fun Preview() {
    DialogPreviewColumn {
        val dialog =
            RepositoryUnlockDialog(RepositoryURI.fromFull("grpc://my-nas:24202/archives/1"))

        dialog.renderDialogCard(
            RepositoryUnlockDialog.State(
                repositoryInfo = RepositoryInformation(null, "Documents"),
                needsUnlock = true,
                canUnlockCredentials = false,
                launchable = mockLaunchable(false, null),
                basicAuthCredentialsState = mutableStateOf(null),
                unlockOptionsState = mutableStateOf(UnlockOptions(rememberSession = false)),
                onClose = {},
            ),
        )
    }
}
