package org.archivekeep.app.desktop.ui.dialogs.repository.registry

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
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
import org.archivekeep.app.core.operations.AddRemoteRepositoryOperation
import org.archivekeep.app.core.operations.AddRemoteRepositoryOperation.AddStatus
import org.archivekeep.app.desktop.domain.wiring.LocalOperationFactory
import org.archivekeep.app.desktop.domain.wiring.OperationFactory
import org.archivekeep.app.desktop.ui.components.errors.AutomaticErrorMessage
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlay
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

class AddRemoteRepositoryDialog : Dialog {
    class VM(
        val coroutineScope: CoroutineScope,
        val operationFactory: OperationFactory,
    ) {
        val uriTextState = mutableStateOf("")
        val basicAuthCredentialsState = mutableStateOf<BasicAuthCredentials?>(null)

        var operation by mutableStateOf<AddRemoteRepositoryOperation?>(null)

        fun launchAdd() {
            operation =
                operationFactory.get(AddRemoteRepositoryOperation.Factory::class.java).create(
                    coroutineScope,
                    uriTextState.value,
                    basicAuthCredentialsState.value,
                )
        }
    }

    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val operationFactory = LocalOperationFactory.current

        val coroutineScope = rememberCoroutineScope()
        val vm = remember(coroutineScope, operationFactory) { VM(coroutineScope, operationFactory) }

        val addStatus =
            vm.operation
                ?.addStatus
                ?.collectAsState(null)
                ?.value

        DialogOverlay(onDismissRequest = onClose) {
            AddRemoteRepositoryDialogContents(
                vm.uriTextState,
                vm.basicAuthCredentialsState,
                addStatus,
                vm::launchAdd,
                onClose,
            )
        }
    }
}

@Composable
private fun AddRemoteRepositoryDialogContents(
    uriTextState: MutableState<String>,
    basicAuthCredentialsState: MutableState<BasicAuthCredentials?>,
    addStatus: AddStatus?,
    launchAdd: () -> Unit,
    onClose: () -> Unit,
) {
    var uriText by uriTextState
    var basicAuthCredentials by basicAuthCredentialsState

    val authNeededOrWasNeeded =
        addStatus is AddStatus.RequiresCredentials ||
            addStatus is AddStatus.WrongCredentials ||
            basicAuthCredentials != null

    DialogCard {
        DialogInnerContainer(
            buildAnnotatedString {
                append("Add remote repository")
            },
            content = {
                Text(
                    "Self-hostable server is work in progress (there's old implementation in Go)...",
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                OutlinedTextField(
                    uriText,
                    onValueChange = { uriText = it },
                    placeholder = {
                        Text("Enter URI of repository to add ...")
                    },
                    singleLine = true,
                    enabled = addStatus !is AddStatus.Adding,
                )
                if (authNeededOrWasNeeded) {
                    Spacer(Modifier.height(12.dp))
                    Text("Authentication needed")
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
                }
                if (addStatus is AddStatus.AddFailed) {
                    AutomaticErrorMessage(addStatus.cause, onResolve = { TODO() })
                }
            },
            bottomContent = {
                DialogButtonContainer {
                    if (authNeededOrWasNeeded) {
                        DialogPrimaryButton(
                            "Authenticate & add",
                            onClick = launchAdd,
                            enabled =
                                addStatus !is AddStatus.Adding &&
                                    basicAuthCredentials?.let { it.username.isNotBlank() && it.password.isNotBlank() } ?: false,
                        )
                    } else {
                        DialogPrimaryButton(
                            "Add",
                            onClick = launchAdd,
                            enabled = uriText.isNotBlank() && addStatus !is AddStatus.Adding,
                        )
                    }

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

@Composable
@Preview
private fun Preview() {
    DialogPreviewColumn {
        AddRemoteRepositoryDialogContents(
            mutableStateOf(""),
            mutableStateOf(null),
            null,
            launchAdd = {},
            onClose = {},
        )
        AddRemoteRepositoryDialogContents(
            mutableStateOf("grpc://my-nas:24202/archives/1"),
            mutableStateOf(null),
            null,
            launchAdd = {},
            onClose = {},
        )
    }
}
