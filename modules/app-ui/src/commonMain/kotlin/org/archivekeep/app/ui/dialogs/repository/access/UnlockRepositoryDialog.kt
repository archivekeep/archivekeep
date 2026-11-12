package org.archivekeep.app.ui.dialogs.repository.access

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Lock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.archivekeep.app.core.api.repository.location.PasswordRequest
import org.archivekeep.app.core.api.repository.location.UserCredentialsRequest
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.asLoadableUnlockRequest
import org.archivekeep.app.core.domain.storages.asUnlockRequest
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.elements.SuccessAlert
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.designsystem.input.TextField
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogDoneButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.data.canUnlockFlow
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.WalletOperationLaunchers
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_authenticate
import org.archivekeep.ui.resources.common_enter_password
import org.archivekeep.ui.resources.common_enter_username
import org.archivekeep.ui.resources.common_remember_password
import org.archivekeep.ui.resources.common_remember_session
import org.archivekeep.ui.resources.dialog_unlock_auth_needed
import org.archivekeep.ui.resources.dialog_unlock_enter_credentials
import org.archivekeep.ui.resources.dialog_unlock_enter_password
import org.archivekeep.ui.resources.dialog_unlock_not_locked
import org.archivekeep.ui.resources.dialog_unlock_password_needed
import org.archivekeep.ui.resources.dialog_unlock_repository_title
import org.archivekeep.ui.resources.dialog_unlock_success
import org.archivekeep.ui.resources.dialog_unlock_wallet_available
import org.archivekeep.ui.resources.dialog_unlock_wallet_hint
import org.archivekeep.ui.resources.dialog_unlock_wallet_locked
import org.archivekeep.ui.resources.dialog_unlock_wallet_open
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedDataStore
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.jetbrains.compose.resources.stringResource

class UnlockRepositoryDialog(
    uri: RepositoryURI,
    val onUnlock: (() -> Unit)? = null,
) : AbstractRepositoryDialog<UnlockRepositoryDialog.State, UnlockRepositoryDialog.VM>(uri) {
    data class State(
        val repositoryInfo: RepositoryInformation,
        val accessState: RepositoryAccessState,
        val canUnlockCredentials: Boolean,
        val launchable: Launchable<Unit>,
        val passwordState: MutableState<String>,
        val rememberPasswordState: MutableState<Boolean>,
        val basicAuthCredentialsState: MutableState<BasicAuthCredentials?>,
        val unlockOptionsState: MutableState<UnlockOptions>,
        val onClose: () -> Unit,
    ) {
        val unlockRequest = accessState.asLoadableUnlockRequest()

        val needsUnlock = unlockRequest.mapIfLoadedOrDefault(false) { it != null }

        var password by passwordState
        var rememberPassword by rememberPasswordState

        var basicAuthCredentials by basicAuthCredentialsState
        var unlockOptions by unlockOptionsState

        val action by launchable.asAction(
            onLaunch = { onLaunch(Unit) },
            canLaunch = {
                when (accessState.asUnlockRequest()) {
                    is UserCredentialsRequest -> {
                        basicAuthCredentials?.let { it.username.isNotBlank() && it.password.isNotBlank() } ?: false
                    }

                    is PasswordRequest -> {
                        password.isNotBlank()
                    }

                    else -> {
                        false
                    }
                }
            },
        )

    }

    @Composable
    override fun title(state: State) =
        buildAnnotatedString {
            append(stringResource(Res.string.dialog_unlock_repository_title))
        }

    inner class VM(
        val coroutineScope: CoroutineScope,
        val repository: Repository,
        val walletOperationLaunchers: WalletOperationLaunchers,
        val credentialStorage: PasswordProtectedDataStore<WalletPO>?,
        val _onClose: () -> Unit,
    ) : IVM {
        val accessState = repository.optionalAccessorFlow

        val passwordState = mutableStateOf("")
        val rememberPasswordState = mutableStateOf(false)

        val basicAuthCredentialsState = mutableStateOf<BasicAuthCredentials?>(null)

        val unlockOptionsState =
            mutableStateOf(
                UnlockOptions(
                    preserveCredentials = true,
                    permanentCredentialsPreserve = false,
                ),
            )
        var unlockOptions by unlockOptionsState

        val unlockAction =
            simpleLaunchable(coroutineScope) { _: Unit ->
                if (unlockOptions.permanentCredentialsPreserve || rememberPasswordState.value) {
                    if (!walletOperationLaunchers.ensureWalletForWrite()) {
                        throw RuntimeException("Wallet not available")
                    }
                }

                when (val request = accessState.first().asUnlockRequest()) {
                    null -> {
                        TODO()
                    }

                    is UserCredentialsRequest -> {
                        request.tryOpen(
                            basicAuthCredentialsState.value!!,
                            unlockOptions,
                        )
                    }

                    is PasswordRequest -> {
                        request.providePassword(passwordState.value, rememberPasswordState.value)
                    }
                }

                onClose()
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
        val credentialStorage = LocalApplicationServices.current.walletDataStore

        return remember {
            VM(
                scope,
                repository,
                walletOperationLaunchers,
                credentialStorage as? PasswordProtectedDataStore,
                onClose,
            )
        }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember(vm) {
            combine(
                vm.repository.resolvedState,
                vm.accessState,
                vm.credentialStorage.canUnlockFlow(),
            ) { resolvedState, accessState, canUnlockCredentials ->
                State(
                    repositoryInfo = resolvedState.information,
                    accessState = accessState,
                    canUnlockCredentials = canUnlockCredentials,
                    launchable = vm.unlockAction,
                    passwordState = vm.passwordState,
                    rememberPasswordState = vm.rememberPasswordState,
                    basicAuthCredentialsState = vm.basicAuthCredentialsState,
                    unlockOptionsState = vm.unlockOptionsState,
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        if (state.launchable.executionOutcome.value is ExecutionOutcome.Success) {
            Text(
                buildAnnotatedString {
                    append(stringResource(Res.string.dialog_unlock_success, state.repositoryInfo.displayName))
                },
            )
            return
        }

        val walletOperationLaunchers = LocalWalletOperationLaunchers.current

        @Composable
        fun WalletOpenBlock() {
            if (state.canUnlockCredentials) {
                Spacer(Modifier.size(12.dp))
                SuccessAlert {
                    Text(
                        stringResource(Res.string.dialog_unlock_wallet_available),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        stringResource(Res.string.dialog_unlock_wallet_hint),
                        modifier = Modifier.padding(bottom = 12.dp),
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
                            contentDescription = stringResource(Res.string.dialog_unlock_wallet_locked),
                            Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))

                        Text(stringResource(Res.string.dialog_unlock_wallet_open))
                    }
                }
                Spacer(Modifier.size(24.dp))
            }
        }

        LoadableGuard(state.unlockRequest) { unlockRequest ->
            when (unlockRequest) {
                null -> {
                    Text(
                        buildAnnotatedString {
                            append(stringResource(Res.string.dialog_unlock_not_locked, state.repositoryInfo.displayName))
                        },
                    )
                }

                is UserCredentialsRequest -> {
                    Text(
                        buildAnnotatedString {
                            append(stringResource(Res.string.dialog_unlock_auth_needed, state.repositoryInfo.displayName))
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    WalletOpenBlock()
                    Text(
                        stringResource(Res.string.dialog_unlock_enter_credentials),
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
                            Text(stringResource(Res.string.common_enter_username))
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
                            Text(stringResource(Res.string.common_enter_password))
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    CheckboxWithText(
                        state.unlockOptions.permanentCredentialsPreserve,
                        onValueChange = {
                            state.unlockOptions =
                                state.unlockOptions.copy(
                                    permanentCredentialsPreserve = it,
                                )
                        },
                        text = stringResource(Res.string.common_remember_session),
                    )
                }

                is PasswordRequest -> {
                    Text(
                        buildAnnotatedString {
                            append(stringResource(Res.string.dialog_unlock_password_needed, state.repositoryInfo.displayName))
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    WalletOpenBlock()
                    Text(
                        stringResource(Res.string.dialog_unlock_enter_password),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    PasswordField(
                        state.password,
                        onValueChange = { state.password = it },
                        placeholder = {
                            Text(stringResource(Res.string.common_enter_password))
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    CheckboxWithText(
                        state.rememberPassword,
                        onValueChange = {
                            state.rememberPassword = !state.rememberPassword
                        },
                        text = stringResource(Res.string.common_remember_password),
                    )
                }
            }
        }

        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        if (state.launchable.executionOutcome.value is ExecutionOutcome.Success || !state.needsUnlock) {
            SimpleActionDialogDoneButtons(onClose = state.onClose)
        } else {
            SimpleActionDialogControlButtons(
                stringResource(Res.string.common_authenticate),
                actionState = state.action,
                onClose = state.onClose,
            )
        }
    }
}
