package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.operations.AddRemoteRepositoryOutcome
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCase
import org.archivekeep.app.core.operations.RequiresCredentialsException
import org.archivekeep.app.core.operations.addS3
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.ui.components.designsystem.elements.WarningAlert
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.designsystem.input.RadioWithTextAndExtra
import org.archivekeep.app.ui.components.designsystem.input.TextField
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogDoneButtons
import org.archivekeep.app.ui.components.feature.errors.AutomaticErrorMessage
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.app.ui.domain.wiring.LocalOperationFactory
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.WalletOperationLaunchers
import org.archivekeep.app.ui.utils.SingleLaunchGuard
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.archivekeep.utils.exceptions.WrongCredentialsException

class AddRemoteRepositoryDialog : Dialog {
    enum class InitType {
        PLAIN,
        ENCRYPTED,
    }

    class Input(
        val selectedRemoteType: MutableState<RemoteType> = mutableStateOf(RemoteType.S3),
        val s3input: S3 = S3(),
        val otherInput: Other = Other(),
    ) {
        enum class RemoteType {
            S3,
            OTHER,
        }

        sealed class RemoteInput(
            val rememberCredentials: MutableState<Boolean> = mutableStateOf(false),
        ) {
            abstract fun canLaunch(): Boolean

            abstract suspend fun execute(useCase: AddRemoteRepositoryUseCase): AddRemoteRepositoryOutcome
        }

        class S3(
            val endpoint: MutableState<String> = mutableStateOf(""),
            val bucket: MutableState<String> = mutableStateOf(""),
            val accessKey: MutableState<String> = mutableStateOf(""),
            val secretKey: MutableState<String> = mutableStateOf(""),
        ) : RemoteInput() {
            override fun canLaunch(): Boolean = endpoint.value.isNotBlank()

            override suspend fun execute(useCase: AddRemoteRepositoryUseCase) =
                useCase.addS3(
                    endpoint.value,
                    bucket.value,
                    accessKey.value,
                    secretKey.value,
                    rememberCredentials.value,
                )
        }

        class Other(
            val uri: MutableState<String> = mutableStateOf(""),
            val basicAuthCredentialsState: MutableState<BasicAuthCredentials?> = mutableStateOf(null),
        ) : RemoteInput() {
            override fun canLaunch(): Boolean = uri.value.isNotBlank()

            override suspend fun execute(useCase: AddRemoteRepositoryUseCase) =
                useCase(RepositoryURI.fromFull(uri.value.trim()), basicAuthCredentialsState.value, rememberCredentials.value)
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
        val walletOperationLaunchers: WalletOperationLaunchers,
    ) {
        val input = Input()

        // TODO: refactor - combine with execution outcome of launchGuard
        val addOutcome = mutableStateOf<AddRemoteRepositoryOutcome?>(null)
        val addLaunchGuard = SingleLaunchGuard(coroutineScope)

        val initType = mutableStateOf<InitType?>(null)
        val initPassword = mutableStateOf("")
        val initPasswordCheck = mutableStateOf("")

        val createLaunchGuard = SingleLaunchGuard(coroutineScope)

        val canLaunchInit =
            derivedStateOf {
                addOutcome.value is AddRemoteRepositoryOutcome.NeedsInitialization &&
                    (
                        initType.value == InitType.PLAIN ||
                            (
                                initType.value == InitType.ENCRYPTED && initPassword.value.isNotBlank() && initPassword.value == initPasswordCheck.value
                            )
                    )
            }

        fun launchAdd() {
            addLaunchGuard.launch {
                addOutcome.value = null

                if (input.currentInput.value.rememberCredentials.value) {
                    if (!walletOperationLaunchers.ensureWalletForWrite()) {
                        throw RuntimeException("Wallet not available")
                    }
                }

                addOutcome.value = input.currentInput.value.execute(useCase)
            }
        }

        fun launchCreate() {
            createLaunchGuard.launch {
                if (input.currentInput.value.rememberCredentials.value) {
                    if (!walletOperationLaunchers.ensureWalletForWrite()) {
                        throw RuntimeException("Wallet not available")
                    }
                }

                val initializer = (addOutcome.value as AddRemoteRepositoryOutcome.NeedsInitialization)
                when (initType.value) {
                    InitType.PLAIN -> initializer.initializeAsPlain!!.invoke()
                    InitType.ENCRYPTED -> initializer.initializeAsE2EEPasswordProtected!!.invoke(initPassword.value)
                    null -> throw IllegalStateException()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun render(onClose: () -> Unit) {
        val operationFactory = LocalOperationFactory.current
        val walletOperationLaunchers = LocalWalletOperationLaunchers.current

        val coroutineScope = rememberCoroutineScope()
        val vm =
            remember(coroutineScope, operationFactory, walletOperationLaunchers) {
                VM(coroutineScope, operationFactory.get(AddRemoteRepositoryUseCase::class.java), walletOperationLaunchers)
            }

        val executionState = vm.addLaunchGuard.state

        val isRunning = vm.addLaunchGuard.runningJob != null
        val isSuccess = vm.addLaunchGuard.executionOutcome.value is ExecutionOutcome.Success
        val isEditable = !isRunning && !isSuccess

        val initializationNeeded =
            vm.addOutcome.value is AddRemoteRepositoryOutcome.NeedsInitialization &&
                (vm.createLaunchGuard.executionOutcome.value !is ExecutionOutcome.Success)
        val initIsEditble = vm.createLaunchGuard.runningJob == null || vm.createLaunchGuard.executionOutcome.value !is ExecutionOutcome.Success

        DialogOverlayCard(onDismissRequest = onClose) {
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
                                S3Form(vm.input.s3input, isEditable)
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
                                    CheckboxWithText(
                                        vm.input.otherInput.rememberCredentials.value,
                                        onValueChange = {
                                            vm.input.otherInput.rememberCredentials.value = it
                                        },
                                        text = "Remember credentials",
                                    )
                                }
                            }
                        }

                        if (error != null) {
                            AutomaticErrorMessage(error.cause, onResolve = { vm.addLaunchGuard.reset() })
                        }

                        if (isSuccess) {
                            if (!initializationNeeded) {
                                Spacer(Modifier.height(12.dp))
                                Text("Remote repository successfully added")
                            } else {
                                Text("Needs init")

                                Spacer(Modifier.height(12.dp))
                                RadioWithTextAndExtra(
                                    selected = vm.initType.value == InitType.PLAIN,
                                    onClick = { vm.initType.value = InitType.PLAIN },
                                    text = "Plain objects (unencrypted, normal access)",
                                    enabled = initIsEditble,
                                    extra = {
                                        if (vm.initType.value == InitType.PLAIN) {
                                            WarningAlert {
                                                Column {
                                                    Text("Your data could be accessed be server owner (service provider).")
                                                    Spacer(Modifier.height(8.dp))
                                                    Text("Do not use for sensitive data with servers (service providers) you don't trust.")
                                                }
                                            }
                                        }
                                    },
                                )
                                RadioWithTextAndExtra(
                                    selected = vm.initType.value == InitType.ENCRYPTED,
                                    onClick = { vm.initType.value = InitType.ENCRYPTED },
                                    text = "Encrypted (custom format)",
                                    enabled = initIsEditble,
                                    extra = {
                                        if (vm.initType.value == InitType.ENCRYPTED) {
                                            Column {
                                                WarningAlert {
                                                    Text("Contents are E2E encrypted. Filenames are not.",)
                                                    Spacer(Modifier.height(8.dp))
                                                    Text("Do not have sensitive data in filenames with servers (service providers) you don't trust.")
                                                }

                                                Spacer(Modifier.height(8.dp))

                                                PasswordField(
                                                    vm.initPassword.value,
                                                    onValueChange = { vm.initPassword.value = it },
                                                    placeholder = { Text("Enter password ...") },
                                                    modifier = Modifier.padding(bottom = 8.dp).testTag("Enter password ..."),
                                                    enabled = initIsEditble,
                                                )

                                                PasswordField(
                                                    vm.initPasswordCheck.value,
                                                    onValueChange = { vm.initPasswordCheck.value = it },
                                                    placeholder = { Text("Verify password ...") },
                                                    modifier = Modifier.testTag("Verify password ..."),
                                                    enabled = initIsEditble,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                bottomContent = {
                    DialogButtonContainer {
                        if (isSuccess) {
                            if (!initializationNeeded) {
                                SimpleActionDialogDoneButtons(onClose)
                            } else {
                                SimpleActionDialogControlButtons(
                                    "Init",
                                    onLaunch = vm::launchCreate,
                                    onClose = onClose,
                                    canLaunch = vm.canLaunchInit.value,
                                    isRunning = isRunning,
                                )
                            }
                        } else {
                            SimpleActionDialogControlButtons(
                                "Add",
                                onLaunch = vm::launchAdd,
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
