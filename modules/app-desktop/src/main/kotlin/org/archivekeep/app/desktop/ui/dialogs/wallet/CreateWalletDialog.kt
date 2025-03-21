package org.archivekeep.app.desktop.ui.dialogs.wallet

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.designsystem.input.PasswordField
import org.archivekeep.app.desktop.ui.dialogs.AbstractDialog
import org.archivekeep.app.desktop.utils.LaunchableAction
import org.archivekeep.utils.loading.Loadable

class CreateWalletDialog : AbstractDialog<CreateWalletDialog.State, CreateWalletDialog.VM>() {
    class VM(
        scope: CoroutineScope,
        val joseStorage: JoseStorage<Credentials>,
        val _onClose: () -> Unit,
    ) : IVM {
        val createAction = LaunchableAction(scope)

        fun launch(password: String) {
            createAction.launch {
                joseStorage.create(password)
                onClose()
            }
        }

        override fun onClose() {
            _onClose()
        }
    }

    class State(
        val createAction: LaunchableAction,
        val onLaunch: (password: String) -> Unit,
        val onClose: () -> Unit,
        val passwordState: MutableState<String?> = mutableStateOf(null),
        val passwordState2: MutableState<String?> = mutableStateOf(null),
    ) : IState {
        var password by passwordState
        var password2 by passwordState2

        val launchCreate by derivedStateOf {
            password?.let { passwordNotNull ->
                {
                    onLaunch(passwordNotNull)
                }
            }
        }

        val canLaunch by derivedStateOf {
            !createAction.isRunning &&
                launchCreate != null &&
                password == password2
        }

        override val title =
            buildAnnotatedString {
                append("Create wallet")
            }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        onClose: () -> Unit,
    ): VM {
        val joseStorage = LocalWalletDataStore.current
        return remember { VM(scope, joseStorage, onClose) }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> = remember { Loadable.Loaded(State(vm.createAction, vm::launch, vm::onClose)) }

    @Composable
    override fun ColumnScope.renderContent(state: State) {
//                    if (joseStorage.currentState !is JoseStorage.State.NotExisting) {
//                        Text("Wallet already exists")
//                        return@DialogInnerContainer
//                    }

        Text(
            "Wallet needs to be created to store and remember passwords and sessions for repository. " +
                "The wallet is stored locally and is password protected",
        )

        PasswordField(
            state.password ?: "",
            onValueChange = {
                state.password = it
            },
            label = { Text("Password") },
            placeholder = { Text("Enter password  ...") },
        )
        PasswordField(
            state.password2 ?: "",
            onValueChange = {
                state.password2 = it
            },
            label = { Text("Password verify") },
            placeholder = { Text("Enter password to verify...") },
        )
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogPrimaryButton(
            "Authenticate",
            onClick = state.launchCreate ?: {},
            enabled = state.canLaunch,
        )

        Spacer(modifier = Modifier.weight(1f))

        DialogDismissButton(
            "Cancel",
            onClick = state.onClose,
        )
    }
}

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        val dialog = CreateWalletDialog()

        dialog.renderDialogCard(
            CreateWalletDialog.State(
                LaunchableAction(CoroutineScope(Dispatchers.Main)),
                onLaunch = {},
                onClose = {},
            ),
        )
    }
}

@Preview
@Composable
private fun preview2() {
    DialogPreviewColumn {
        val dialog = CreateWalletDialog()

        dialog.renderDialogCard(
            CreateWalletDialog.State(
                LaunchableAction(CoroutineScope(Dispatchers.Main)),
                onLaunch = {},
                onClose = {},
                passwordState = mutableStateOf("The first password"),
            ),
        )
    }
}

@Preview
@Composable
private fun preview3() {
    DialogPreviewColumn {
        val dialog = CreateWalletDialog()

        dialog.renderDialogCard(
            CreateWalletDialog.State(
                LaunchableAction(CoroutineScope(Dispatchers.Main)),
                onLaunch = {},
                onClose = {},
                passwordState = mutableStateOf("The first password"),
                passwordState2 = mutableStateOf("The first"),
            ),
        )
    }
}

@Preview
@Composable
private fun preview4() {
    DialogPreviewColumn {
        val dialog = CreateWalletDialog()

        dialog.renderDialogCard(
            CreateWalletDialog.State(
                LaunchableAction(CoroutineScope(Dispatchers.Main)),
                onLaunch = {},
                onClose = {},
                passwordState = mutableStateOf("The same password"),
                passwordState2 = mutableStateOf("The same password"),
            ),
        )
    }
}
