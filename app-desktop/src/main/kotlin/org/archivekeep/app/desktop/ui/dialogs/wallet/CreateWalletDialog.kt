package org.archivekeep.app.desktop.ui.dialogs.wallet

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.domain.wiring.LocalWalletDataStore
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.rememberLaunchableAction

class CreateWalletDialog : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val joseStorage = LocalWalletDataStore.current

        val createAction = rememberLaunchableAction()

        var password by remember {
            mutableStateOf<String?>(null)
        }
        var password2 by remember {
            mutableStateOf<String?>(null)
        }

        val launchCreate =
            password?.let { passwordNotNull ->
                {
                    createAction.launch {
                        joseStorage.create(passwordNotNull)
                        onClose()
                    }
                }
            }

        DialogOverlayCard(onDismissRequest = onClose) {
            DialogInnerContainer(
                buildAnnotatedString {
                    append("Create wallet")
                },
                content = {
//                    if (joseStorage.currentState !is JoseStorage.State.NotExisting) {
//                        Text("Wallet already exists")
//                        return@DialogInnerContainer
//                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        password ?: "",
                        onValueChange = {
                            password = it
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = {
                            Text("Enter password  ...")
                        },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        password2 ?: "",
                        onValueChange = {
                            password2 = it
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        placeholder = {
                            Text("Enter password to verify...")
                        },
                        singleLine = true,
                    )
                },
                bottomContent = {
                    DialogButtonContainer {
                        DialogPrimaryButton(
                            "Authenticate",
                            onClick = launchCreate ?: {},
                            enabled =
                                !createAction.isRunning &&
                                    launchCreate != null &&
                                    password == password2,
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
