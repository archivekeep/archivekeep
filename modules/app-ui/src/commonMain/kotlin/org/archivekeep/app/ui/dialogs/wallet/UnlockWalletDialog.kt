package org.archivekeep.app.ui.dialogs.wallet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.ui.components.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.dialogs.AbstractDialog
import org.archivekeep.app.ui.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.mockLaunchable
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.ui.tooling.preview.Preview

class UnlockWalletDialog(
    val onUnlock: (() -> Unit)?,
) : AbstractDialog<UnlockWalletDialog.State, UnlockWalletDialog.VM>() {
    inner class VM(
        scope: CoroutineScope,
        val joseStorage: JoseStorage<Credentials>,
        val _onClose: () -> Unit,
    ) : IVM {
        val launchable =
            simpleLaunchable(scope) { password: String ->
                joseStorage.unlock(password)

                try {
                    onClose()
                    onUnlock?.let { it() }
                } catch (e: Throwable) {
                    println("ERROR: close or onUnlock failed: $e")
                    e.printStackTrace()
                }
            }

        override fun onClose() {
            _onClose()
        }
    }

    class State(
        val launchable: Launchable<String>,
        val onClose: () -> Unit,
    ) : IState {
        var password by mutableStateOf<String?>(null)

        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Open wallet")
            }

        val action =
            launchable.asAction(
                onLaunch = { password!!.let { onLaunch(it) } },
                canLaunch = { password?.isNotBlank() ?: false },
            )
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
                    vm.launchable,
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

        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        SimpleActionDialogControlButtons(
            "Authenticate",
            actionState = state.action.value,
            onClose = state.onClose,
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
                mockLaunchable(false, null),
                onClose = {},
            ),
        )
    }
}
