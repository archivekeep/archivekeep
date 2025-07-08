package org.archivekeep.app.ui.dialogs.repository.access

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
import kotlinx.coroutines.flow.first
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.asLoadableUnlockRequest
import org.archivekeep.app.core.domain.storages.asUnlockRequest
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
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
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedDataStore
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

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
        val basicAuthCredentialsState: MutableState<BasicAuthCredentials?>,
        val unlockOptionsState: MutableState<UnlockOptions>,
        val onClose: () -> Unit,
    ) : IState {
        val unlockRequest = accessState.asLoadableUnlockRequest()

        val needsUnlock = unlockRequest.mapIfLoadedOrDefault(false) { it != null }

        var password by passwordState
        var basicAuthCredentials by basicAuthCredentialsState
        var unlockOptions by unlockOptionsState

        val action by launchable.asAction(
            onLaunch = { onLaunch(Unit) },
            canLaunch = {
                when (accessState.asUnlockRequest()) {
                    is RepoAuthRequest -> {
                        basicAuthCredentials?.let { it.username.isNotBlank() && it.password.isNotBlank() } ?: false
                    }
                    is FileSystemStorageDriver.PasswordRequest -> {
                        password.isNotBlank()
                    }
                    else -> false
                }
            },
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
        val credentialStorage: PasswordProtectedDataStore<Credentials>?,
        val _onClose: () -> Unit,
    ) : IVM {
        val accessState = repository.optionalAccessorFlow

        val passwordState = mutableStateOf<String>("")
        val basicAuthCredentialsState = mutableStateOf<BasicAuthCredentials?>(null)

        val unlockOptionsState = mutableStateOf(UnlockOptions(rememberSession = false))
        var unlockOptions by unlockOptionsState

        val unlockAction =
            simpleLaunchable(coroutineScope) { _: Unit ->
                if (unlockOptions.rememberSession) {
                    if (!walletOperationLaunchers.ensureWalletForWrite()) {
                        return@simpleLaunchable
                    }
                }

                when (val request = accessState.first().asUnlockRequest()) {
                    null -> TODO()
                    is RepoAuthRequest ->
                        request.tryOpen(
                            basicAuthCredentialsState.value!!,
                            unlockOptions,
                        )

                    is FileSystemStorageDriver.PasswordRequest -> {
                        request.providePassword(passwordState.value)
                    }
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
        val credentialStorage = LocalApplicationServices.current.environment.walletDataStore

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
                    append("Repository ")
                    appendBoldSpan(state.repositoryInfo.displayName)
                    append(" is now unlocked.")
                },
            )
            return
        }

        val walletOperationLaunchers = LocalWalletOperationLaunchers.current

        LoadableGuard(state.unlockRequest) { unlockRequest ->
            println(state.accessState)
            println((state.accessState as? OptionalLoadable.NotAvailable)?.cause)

            when (unlockRequest) {
                null -> {
                    Text(
                        buildAnnotatedString {
                            append("Repository ")
                            appendBoldSpan(state.repositoryInfo.displayName)
                            append(" is not locked. No Action needed.")
                        },
                    )
                }

                is RepoAuthRequest -> {
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
                }

                is FileSystemStorageDriver.PasswordRequest -> {
                    Text(
                        buildAnnotatedString {
                            append("Password is needed to access ")
                            appendBoldSpan(state.repositoryInfo.displayName)
                            append(" repository.")
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    PasswordField(
                        state.password,
                        onValueChange = { state.password = it },
                        placeholder = {
                            Text("Enter password ...")
                        },
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
                "Authenticate",
                actionState = state.action,
                onClose = state.onClose,
            )
        }
    }
}
