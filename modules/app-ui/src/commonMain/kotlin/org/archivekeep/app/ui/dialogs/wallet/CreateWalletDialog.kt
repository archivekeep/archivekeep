package org.archivekeep.app.ui.dialogs.wallet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.dialogs.AbstractDialog
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_authenticate
import org.archivekeep.ui.resources.common_password
import org.archivekeep.ui.resources.dialog_create_wallet_enter_password
import org.archivekeep.ui.resources.dialog_create_wallet_message
import org.archivekeep.ui.resources.dialog_create_wallet_password_verify
import org.archivekeep.ui.resources.dialog_create_wallet_verify_password
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedDataStore
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.resources.stringResource

class CreateWalletDialog : AbstractDialog<CreateWalletDialog.State, CreateWalletDialog.VM>() {
    class VM(
        scope: CoroutineScope,
        val passwordProtectedDataStore: PasswordProtectedDataStore<WalletPO>,
        val _onClose: () -> Unit,
    ) : IVM {
        val launchable =
            simpleLaunchable(scope) { password: String ->
                passwordProtectedDataStore.create(password)
                onClose()
            }

        override fun onClose() {
            _onClose()
        }
    }

    class State(
        val launchable: Launchable<String>,
        val onClose: () -> Unit,
        val passwordState: MutableState<String?> = mutableStateOf(null),
        val passwordState2: MutableState<String?> = mutableStateOf(null),
    ) : IState {
        var password by passwordState
        var password2 by passwordState2

        val action =
            launchable.asAction(
                onLaunch = {
                    password!!.let { passwordNotNull ->
                        onLaunch(passwordNotNull)
                    }
                },
                canLaunch = { (password?.isNotBlank() ?: false) && password == password2 },
            )

        override val title =
            buildAnnotatedString {
                append("Create Wallet")
            }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        onClose: () -> Unit,
    ): VM {
        val applicationServices = LocalApplicationServices.current

        return remember { VM(scope, applicationServices.walletDataStore as PasswordProtectedDataStore, onClose) }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember {
            Loadable.Loaded(State(vm.launchable, vm::onClose))
        }

    @Composable
    override fun ColumnScope.renderContent(state: State) {
//                    if (joseStorage.currentState !is JoseStorage.State.NotExisting) {
//                        Text("Wallet already exists")
//                        return@DialogInnerContainer
//                    }

        Text(
            stringResource(Res.string.dialog_create_wallet_message),
        )

        PasswordField(
            state.password ?: "",
            onValueChange = {
                state.password = it
            },
            label = { Text(stringResource(Res.string.common_password)) },
            placeholder = { Text(stringResource(Res.string.dialog_create_wallet_enter_password)) },
        )
        PasswordField(
            state.password2 ?: "",
            onValueChange = {
                state.password2 = it
            },
            label = { Text(stringResource(Res.string.dialog_create_wallet_password_verify)) },
            placeholder = { Text(stringResource(Res.string.dialog_create_wallet_verify_password)) },
        )
        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        SimpleActionDialogControlButtons(
            stringResource(Res.string.common_authenticate),
            actionState = state.action.value,
            onClose = state.onClose,
        )
    }
}
