package org.archivekeep.app.desktop.ui.dialogs.wallet

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.ui.components.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.desktop.ui.components.errors.AutomaticErrorMessage
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.input.PasswordField
import org.archivekeep.app.desktop.ui.dialogs.AbstractDialog
import org.archivekeep.app.desktop.utils.LaunchableAction
import org.archivekeep.utils.loading.Loadable

class UnlockWalletDialog(
    val onUnlock: (() -> Unit)?,
) : AbstractDialog<UnlockWalletDialog.State, UnlockWalletDialog.VM>() {
    inner class VM(
        scope: CoroutineScope,
        val joseStorage: JoseStorage<Credentials>,
        val _onClose: () -> Unit,
    ) : IVM {
        val openAction =
            LaunchableAction(
                scope = scope,
            )

        var unlockError = mutableStateOf<Throwable?>(null)

        fun launch(password: String) {
            openAction.launch {
                try {
                    joseStorage.unlock(password)
                } catch (e: Throwable) {
                    unlockError.value = e
                    return@launch
                }

                try {
                    onClose()
                    onUnlock?.let { it() }
                } catch (e: Throwable) {
                    println("ERROR: close or onUnlock failed: $e")
                    e.printStackTrace()
                }
            }
        }

        override fun onClose() {
            _onClose()
        }
    }

    class State(
        val openAction: LaunchableAction,
        val unlockError: MutableState<Throwable?>,
        val onLaunch: (password: String) -> Unit,
        val onClose: () -> Unit,
    ) : IState {
        var password by mutableStateOf<String?>(null)

        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Open wallet")
            }

        val launchOpen by derivedStateOf {
            password?.let { password ->
                {
                    onLaunch(password)
                }
            }
        }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        onClose: () -> Unit,
    ): VM {
        val joseStorage = LocalWalletDataStore.current

        return remember(scope, joseStorage, onClose) { VM(scope, joseStorage, onClose) }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember(vm) {
            Loadable.Loaded(
                State(
                    vm.openAction,
                    vm.unlockError,
                    vm::launch,
                    vm::onClose,
                ),
            )
        }

    @Composable
    override fun ColumnScope.renderContent(state: State) {
//                    if (joseStorage.currentState !is JoseStorage.State.Locked) {
//                        Text("Wallet already opened")
//                        return@DialogInnerContainer
//                    }

        Text("Wallet is locked, unlock it to connect to repositories with remembered credentials or stored sessions.")

        PasswordField(
            state.password ?: "",
            onValueChange = {
                state.password = it
            },
            label = { Text("Password") },
            placeholder = { Text("Enter password  ...") },
        )

        state.unlockError.value?.let {
            AutomaticErrorMessage(it, onResolve = { state.unlockError.value = null })
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        SimpleActionDialogControlButtons(
            "Authenticate",
            onLaunch = state.launchOpen ?: {},
            onClose = state.onClose,
            canLaunch =
                !state.openAction.isRunning &&
                    state.launchOpen != null,
        )
    }
}

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        val dialog = UnlockWalletDialog(onUnlock = {})

        dialog.renderDialogCard(
            UnlockWalletDialog.State(
                LaunchableAction(CoroutineScope(Dispatchers.Main)),
                mutableStateOf(null),
                onLaunch = {},
                onClose = {},
            ),
        )
    }
}
