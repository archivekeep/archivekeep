package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCase
import org.archivekeep.app.core.operations.RequiresCredentialsException
import org.archivekeep.app.core.operations.WrongCredentialsException
import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.ui.components.designsystem.elements.WarningAlert
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.designsystem.input.TextField
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogDoneButtons
import org.archivekeep.app.ui.components.feature.errors.AutomaticErrorMessage
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.app.ui.domain.wiring.LocalOperationFactory
import org.archivekeep.app.ui.utils.SingleLaunchGuard
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

class AddRemoteRepositoryDialog : Dialog {
    class Input(
        val selectedRemoteType: MutableState<RemoteType> = mutableStateOf(RemoteType.S3),
        val s3input: S3 = S3(),
        val otherInput: Other = Other(),
    ) {
        enum class RemoteType {
            S3,
            OTHER,
        }

        sealed interface RemoteInput {
            fun canLaunch(): Boolean

            suspend fun execute(useCase: AddRemoteRepositoryUseCase)
        }

        class S3(
            val endpoint: MutableState<String> = mutableStateOf(""),
            val bucket: MutableState<String> = mutableStateOf(""),
            val accessKey: MutableState<String> = mutableStateOf(""),
            val secretKey: MutableState<String> = mutableStateOf(""),
        ) : RemoteInput {
            override fun canLaunch(): Boolean = endpoint.value.isNotBlank()

            override suspend fun execute(addRemoteRepositoryUseCase: AddRemoteRepositoryUseCase) {
                addRemoteRepositoryUseCase(
                    RepositoryURI("s3", S3RepositoryURIData(endpoint.value, bucket.value).serialized()),
                    BasicAuthCredentials(accessKey.value, secretKey.value),
                )
            }
        }

        class Other(
            val uri: MutableState<String> = mutableStateOf(""),
            val basicAuthCredentialsState: MutableState<BasicAuthCredentials?> = mutableStateOf(null),
        ) : RemoteInput {
            override fun canLaunch(): Boolean = uri.value.isNotBlank()

            override suspend fun execute(useCase: AddRemoteRepositoryUseCase) {
                useCase(RepositoryURI.fromFull(uri.value.trim()), basicAuthCredentialsState.value)
            }
        }

        val currentInput =
            derivedStateOf {
                when (selectedRemoteType.value) {
                    RemoteType.S3 -> s3input
                    RemoteType.OTHER -> otherInput
                }
            }
    }

    class VM(
        coroutineScope: CoroutineScope,
        val useCase: AddRemoteRepositoryUseCase,
    ) {
        val input = Input()

        val launchGuard = SingleLaunchGuard(coroutineScope)

        fun launchAdd() {
            launchGuard.launch {
                input.currentInput.value.execute(useCase)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun render(onClose: () -> Unit) {
        val operationFactory = LocalOperationFactory.current

        val coroutineScope = rememberCoroutineScope()
        val vm = remember(coroutineScope, operationFactory) { VM(coroutineScope, operationFactory.get(AddRemoteRepositoryUseCase::class.java)) }

        val executionState = vm.launchGuard.state

        val isRunning = vm.launchGuard.runningJob != null
        val isSuccess = vm.launchGuard.executionOutcome.value is ExecutionOutcome.Success
        val isEditable = !isRunning && !isSuccess

        DialogOverlayCard(onDismissRequest = onClose) {
            val launchAdd = { vm.launchAdd() }
            var uriText by vm.input.otherInput.uri
            var basicAuthCredentials by vm.input.otherInput.basicAuthCredentialsState

            val error = (executionState as? SingleLaunchGuard.State.Completed)?.outcome as? ExecutionOutcome.Failed

            val selectedTab = vm.input.selectedRemoteType.value

            DialogInnerContainer(
                buildAnnotatedString {
                    append("Add remote repository")
                },
                content = {
                    SecondaryTabRow(
                        selectedTab.ordinal,
                        containerColor = Color.White,
                    ) {
                        Tab(
                            selected = selectedTab == Input.RemoteType.S3,
                            onClick = { vm.input.selectedRemoteType.value = Input.RemoteType.S3 },
                            text = {
                                Text(text = "S3", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            enabled = isEditable,
                        )
                        Tab(
                            selected = selectedTab == Input.RemoteType.OTHER,
                            onClick = { vm.input.selectedRemoteType.value = Input.RemoteType.OTHER },
                            text = {
                                Text(text = "Other", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            enabled = isEditable,
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    ScrollableColumn {
                        when (selectedTab) {
                            Input.RemoteType.S3 -> {
                                Text(
                                    "Connection details for S3 bucket:",
                                )
                                Spacer(Modifier.height(4.dp))
                                TextField(
                                    vm.input.s3input.endpoint.value,
                                    onValueChange = { vm.input.s3input.endpoint.value = it },
                                    label = { Text("Endpoint URL") },
                                    placeholder = { Text("Endpoint URL") },
                                    singleLine = true,
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions =
                                        KeyboardOptions(
                                            capitalization = KeyboardCapitalization.None,
                                            keyboardType = KeyboardType.Uri,
                                        ),
                                )
                                if (vm.input.s3input.endpoint.value
                                        .trim()
                                        .startsWith("http://")
                                ) {
                                    Spacer(Modifier.height(8.dp))
                                    WarningAlert {
                                        Column {
                                            Text(
                                                "Insecure protocol is used for endpoint. " +
                                                    "This results in plain data being sent over network, that is readable by anyone.",
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text("It is strongly recommended to connect to this server using a VPN you absolutely trust.")
                                        }
                                    }
                                }
                                TextField(
                                    vm.input.s3input.bucket.value,
                                    onValueChange = { vm.input.s3input.bucket.value = it },
                                    label = { Text("Bucket name") },
                                    placeholder = { Text("Bucket name") },
                                    singleLine = true,
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                TextField(
                                    vm.input.s3input.accessKey.value,
                                    onValueChange = { vm.input.s3input.accessKey.value = it },
                                    label = { Text("Access key") },
                                    placeholder = { Text("Access key") },
                                    singleLine = true,
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                PasswordField(
                                    vm.input.s3input.secretKey.value,
                                    onValueChange = { vm.input.s3input.secretKey.value = it },
                                    label = { Text("Secret key") },
                                    placeholder = { Text("Secret key") },
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(Modifier.height(12.dp))
                                WarningAlert {
                                    Column {
                                        Text(
                                            "There's no E2E encryption yet. Your data could be accessed be server owner (service provider).",
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text("Do not use for sensitive data with servers (service providers) you don't trust.")
                                    }
                                }
                            }

                            Input.RemoteType.OTHER -> {
                                val authNeededOrWasNeeded =
                                    error?.cause is RequiresCredentialsException ||
                                        error?.cause is WrongCredentialsException ||
                                        basicAuthCredentials != null

                                Text(
                                    "Self-hostable server is work in progress (there's an old discontinued server implementation written in Go)...",
                                )
                                Spacer(Modifier.height(4.dp))
                                TextField(
                                    uriText,
                                    onValueChange = { uriText = it },
                                    label = { Text("Remote repository URL") },
                                    placeholder = { Text("Enter URI of repository to add ...") },
                                    singleLine = true,
                                    enabled = isEditable,
                                    modifier = Modifier.fillMaxWidth(),
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
                            }
                        }

                        if (error != null) {
                            AutomaticErrorMessage(error.cause, onResolve = { vm.launchGuard.reset() })
                        }

                        if (isSuccess) {
                            Spacer(Modifier.height(12.dp))
                            Text("Remote repository successfully added")
                        }
                    }
                },
                bottomContent = {
                    DialogButtonContainer {
                        if (isSuccess) {
                            SimpleActionDialogDoneButtons(onClose)
                        } else {
                            SimpleActionDialogControlButtons(
                                "Add",
                                onLaunch = launchAdd,
                                onClose = onClose,
                                canLaunch =
                                    vm.input.currentInput.value
                                        .canLaunch() &&
                                        !isRunning,
                                isRunning = isRunning,
                            )
                        }
                    }
                },
            )
        }
    }
}
