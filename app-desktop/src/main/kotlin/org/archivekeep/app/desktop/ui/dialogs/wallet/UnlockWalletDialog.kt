package org.archivekeep.app.desktop.ui.dialogs.wallet

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.ui.components.errors.AutomaticErrorMessage
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.LaunchableAction

class UnlockWalletDialog(
    val onUnlock: (() -> Unit)?,
) : Dialog {
    inner class VM(
        scope: CoroutineScope,
        val joseStorage: JoseStorage<Credentials>,
        val onClose: () -> Unit,
    ) {
        val openAction =
            LaunchableAction(
                scope = scope,
            )

        var password by mutableStateOf<String?>(null)

        var unlockError by mutableStateOf<Throwable?>(null)

        val launchOpen by derivedStateOf {
            password?.let { password ->
                {
                    openAction.launch {
                        try {
                            joseStorage.unlock(password)
                        } catch (e: Throwable) {
                            unlockError = e
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
            }
        }
    }

    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val joseStorage = LocalWalletDataStore.current

        val scope = rememberCoroutineScope()
        val vm = remember(scope, joseStorage, onClose) { VM(scope, joseStorage, onClose) }

        DialogOverlayCard(onDismissRequest = onClose) {
            DialogInnerContainer(
                buildAnnotatedString {
                    append("Open wallet")
                },
                content = {
//                    if (joseStorage.currentState !is JoseStorage.State.Locked) {
//                        Text("Wallet already opened")
//                        return@DialogInnerContainer
//                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        vm.password ?: "",
                        onValueChange = {
                            vm.password = it
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = {
                            Text("Enter password  ...")
                        },
                        singleLine = true,
                    )

                    vm.unlockError?.let {
                        AutomaticErrorMessage(it, onResolve = { vm.unlockError = null })
                    }
                },
                bottomContent = {
                    DialogButtonContainer {
                        DialogPrimaryButton(
                            "Authenticate",
                            onClick = vm.launchOpen ?: {},
                            enabled =
                                !vm.openAction.isRunning &&
                                    vm.launchOpen != null,
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
