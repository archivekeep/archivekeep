package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.StorageMarking
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryUseCase
import org.archivekeep.app.core.utils.generics.Execution
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCard
import org.archivekeep.app.ui.components.designsystem.dialog.DialogFilePicker
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInputLabel
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlay
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.designsystem.input.RadioWithText
import org.archivekeep.app.ui.components.designsystem.input.RadioWithTextAndExtra
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogDoneButtons
import org.archivekeep.app.ui.components.feature.errors.AutomaticErrorMessage
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.app.ui.domain.wiring.LocalOperationFactory
import org.archivekeep.app.ui.domain.wiring.OperationFactory
import org.archivekeep.app.ui.utils.SingleLaunchGuard
import org.archivekeep.app.ui.utils.filesystem.LocalFilesystemDirectoryPicker
import org.archivekeep.app.ui.utils.filesystem.PickResult
import org.archivekeep.utils.loading.Loadable

class AddFileSystemRepositoryDialog(
    val intendedStorageType: FileSystemStorageType?,
) : Dialog {
    inner class VM(
        val coroutineScope: CoroutineScope,
        val operationFactory: OperationFactory,
    ) : RememberObserver {
        var pickResult by mutableStateOf<PickResult?>(null)
            private set

        var markConfirmed by mutableStateOf<Boolean?>(false)

        private var operationScope: CoroutineScope = coroutineScope + SupervisorJob()

        var addOperation by mutableStateOf<Loadable<AddFileSystemRepositoryOperation>?>(null)
            private set

        fun setPick(result: PickResult) {
            pickResult = result
            markConfirmed = null

            operationScope.cancel()
            operationScope = coroutineScope + SupervisorJob()

            when (result) {
                is PickResult.Success -> {
                    addOperation = Loadable.Loading

                    operationScope.launch {
                        try {
                            val newOperation =
                                operationFactory
                                    .get(AddFileSystemRepositoryUseCase::class.java)
                                    .begin(
                                        operationScope,
                                        result.path,
                                        intendedStorageType,
                                    )

                            ensureActive()
                            addOperation = Loadable.Loaded(newOperation)
                        } catch (e: Throwable) {
                            ensureActive()
                            addOperation = Loadable.Failed(e)
                        }
                    }
                }

                is PickResult.Failure -> {
                    addOperation = null
                }
            }
        }

        override fun onAbandoned() {
            operationScope.cancel()
        }

        override fun onForgotten() {
            operationScope.cancel()
        }

        override fun onRemembered() {}
    }

    @Composable
    override fun render(onClose: () -> Unit) {
        val operationFactory = LocalOperationFactory.current

        val coroutineScope = rememberCoroutineScope()

        val vm =
            remember(coroutineScope, operationFactory) {
                VM(
                    coroutineScope,
                    operationFactory,
                )
            }

        val permissionGrant = platformSpecificFileSystemRepositoryGuard()

        val onLaunch = LocalFilesystemDirectoryPicker.current(vm::setPick)

        LaunchedEffect(permissionGrant) {
            if (permissionGrant == PlatformSpecificPermissionFulfilment.IsFine) {
                onLaunch()
            }
        }

        val selectedPath = vm.pickResult

        DialogOverlay(onDismissRequest = onClose) {
            AddRepositoryDialogContents(
                intendedStorageType,
                permissionGrant,
                selectedPath,
                onLaunch,
                vm.markConfirmed,
                { vm.markConfirmed = it },
                vm.addOperation,
                onClose,
            )
        }
    }
}

sealed interface PlatformSpecificPermissionFulfilment {
    data object IsFine : PlatformSpecificPermissionFulfilment

    data class NeedsGrant(
        val texts: List<String>,
        val buttonText: String,
        val onLaunch: () -> Unit,
    ) : PlatformSpecificPermissionFulfilment
}

@Composable
expect fun platformSpecificFileSystemRepositoryGuard(): PlatformSpecificPermissionFulfilment

@Composable
private fun AddRepositoryDialogContents(
    intendedStorageType: FileSystemStorageType?,
    permissionFulfilment: PlatformSpecificPermissionFulfilment,
    pick: PickResult?,
    onTriggerChange: () -> Unit,
    markConfirmed: Boolean?,
    setMarkConfirmed: (newValue: Boolean?) -> Unit,
    optionLoadable: Loadable<AddFileSystemRepositoryOperation>?,
    onClose: () -> Unit,
) {
    var createEncrypted by mutableStateOf(false)
    var createPassword by mutableStateOf("")
    var createPassword2 by mutableStateOf("")

    var password by mutableStateOf("")
    val coroutineScope = rememberCoroutineScope()
    val passwordLaunchGuard = remember(coroutineScope) { SingleLaunchGuard(coroutineScope) }
    val singleLaunchGuard = remember(coroutineScope) { SingleLaunchGuard(coroutineScope) }

    DialogCard {
        DialogInnerContainer(
            remember(intendedStorageType) {
                buildAnnotatedString {
                    append(
                        when (intendedStorageType) {
                            FileSystemStorageType.LOCAL -> "Add local repository"
                            FileSystemStorageType.EXTERNAL -> "Add external repository"
                            null -> "Add repository"
                        },
                    )
                }
            },
            content = {
                when (permissionFulfilment) {
                    PlatformSpecificPermissionFulfilment.IsFine -> {
                        DialogInputLabel("Repository directory:")

                        DialogFilePicker(
                            pick?.let {
                                when (pick) {
                                    is PickResult.Failure -> "ERROR"
                                    is PickResult.Success -> pick.path
                                }
                            },
                            onTriggerChange = onTriggerChange,
                            // TODO: changeEnabled = initStatus == null && addStatus == null,
                        )
                    }

                    is PlatformSpecificPermissionFulfilment.NeedsGrant -> {
                        HorizontalDivider()
                        Spacer(Modifier)
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            permissionFulfilment.texts.forEach { Text(it) }
                            Button(onClick = permissionFulfilment.onLaunch) { Text(permissionFulfilment.buttonText) }
                        }
                        Spacer(Modifier)
                        HorizontalDivider()
                    }
                }

                if (optionLoadable != null) {
                    LoadableGuard(
                        optionLoadable,
                        loadingContent = {
                            ProgressText("Preparing...")
                        },
                    ) { option ->
                        when (option) {
                            is AddFileSystemRepositoryOperation.Invalid -> {
                                ProgressText("Error $option")
                            }

                            is AddFileSystemRepositoryOperation.DirectoryNotRepository -> {
                                val initStatus = option.initStatus.collectAsState().value
                                val addStatus = option.addStatus.collectAsState().value

                                val canEdit = option.initStatus.collectAsState().value == Execution.NotRunning

                                ProgressText("The directory is not a repository, yet. Continue to initialize it as an archive repository.")

                                Spacer(Modifier.height(12.dp))
                                RadioWithText(
                                    selected = !createEncrypted,
                                    onClick = { createEncrypted = false },
                                    text = "Plain files (unencrypted, normal access)",
                                    enabled = canEdit,
                                )
                                RadioWithTextAndExtra(
                                    selected = createEncrypted,
                                    onClick = { createEncrypted = true },
                                    text = "Encrypted (custom format)",
                                    enabled = canEdit,
                                    extra = {
                                        if (createEncrypted) {
                                            Column {
                                                PasswordField(
                                                    createPassword,
                                                    onValueChange = { createPassword = it },
                                                    placeholder = { Text("Enter password ...") },
                                                    modifier = Modifier.padding(bottom = 8.dp).testTag("Enter password ..."),
                                                    enabled = canEdit,
                                                )

                                                PasswordField(
                                                    createPassword2,
                                                    onValueChange = { createPassword2 = it },
                                                    placeholder = { Text("Verify password ...") },
                                                    modifier = Modifier.padding(bottom = 8.dp).testTag("Verify password ..."),
                                                    enabled = canEdit,
                                                )
                                            }
                                        }
                                    },
                                )

                                InitStatus(option.initStatus.value)
                                AddStatus(option.addStatus.value)

                                if (initStatus == Execution.NotRunning && addStatus == Execution.NotRunning) {
                                    StorageMark(option.storageMarking, markConfirmed, setMarkConfirmed)
                                }
                            }

                            is AddFileSystemRepositoryOperation.PlainFileSystemRepository -> {
                                val addStatus = option.addStatus.collectAsState().value

                                AddStatus(option.addStatus.value, notRunningStatus = { ProgressText("Repository can be added.") })

                                if (addStatus == Execution.NotRunning) {
                                    StorageMark(option.storageMarking, markConfirmed, setMarkConfirmed)
                                }
                            }

                            is AddFileSystemRepositoryOperation.EncryptedFileSystemRepository -> {
                                ProgressText("The repository is encrypted, and password protected.")

                                option.unlockStatus.collectAsState().value.let {
                                    if (it is Execution.InProgress) {
                                        ProgressText("Unlocking...")
                                    } else if (it is Execution.Finished && it.outcome is ExecutionOutcome.Success) {
                                        ProgressText("Successfully unlocked.")
                                    } else {
                                        Text(
                                            "Enter password to access it:",
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                        )
                                        PasswordField(
                                            password,
                                            onValueChange = { password = it },
                                            placeholder = { Text("Enter password ...") },
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        )
                                        Button(
                                            onClick = { passwordLaunchGuard.launch { option.unlock(password) } },
                                            enabled = password.isNotEmpty(),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        ) {
                                            Text("Unlock")
                                        }

                                        passwordLaunchGuard.executionOutcome.value.let {
                                            if (it is ExecutionOutcome.Failed) {
                                                AutomaticErrorMessage(it.cause, onResolve = { singleLaunchGuard.reset() })
                                            }
                                        }
                                    }
                                }

                                AddStatus(option.addStatus.collectAsState().value)
                            }
                        }
                    }
                } else {
                    Text("Please, select directory.")
                }
            },
            bottomContent = {
                DialogButtonContainer {
                    when (optionLoadable) {
                        is Loadable.Failed, null, Loadable.Loading -> {
                            SimpleActionDialogControlButtons(
                                "Add",
                                onLaunch = {},
                                onClose = onClose,
                                canLaunch = false,
                            )
                        }

                        is Loadable.Loaded<*> -> {
                            when (val option = optionLoadable.value) {
                                is AddFileSystemRepositoryOperation.Invalid -> {
                                    SimpleActionDialogControlButtons(
                                        "Add",
                                        onLaunch = {},
                                        onClose = onClose,
                                        canLaunch = false,
                                    )
                                }

                                is AddFileSystemRepositoryOperation.DirectoryNotRepository -> {
                                    val state = option.addStatus.collectAsState().value

                                    if (state is Execution.Finished) {
                                        SimpleActionDialogDoneButtons(onClose)
                                    } else {
                                        SimpleActionDialogControlButtons(
                                            "Init",
                                            onLaunch = {
                                                if (!createEncrypted) {
                                                    singleLaunchGuard.launch { option.startInitAsPlain(markConfirmed) }
                                                } else {
                                                    singleLaunchGuard.launch { option.startInitAsEncrypted(markConfirmed, createPassword) }
                                                }
                                            },
                                            onClose = onClose,
                                            canLaunch =
                                                state == Execution.NotRunning &&
                                                    (!option.storageMarking.isRemark || markConfirmed == true) &&

                                                    (
                                                        !createEncrypted ||
                                                            (
                                                                createPassword.isNotBlank() && createPassword == createPassword2
                                                            )
                                                    ),
                                        )
                                    }
                                }

                                is AddFileSystemRepositoryOperation.PlainFileSystemRepository -> {
                                    val state = option.addStatus.collectAsState().value

                                    if (state is Execution.Finished) {
                                        SimpleActionDialogDoneButtons(onClose)
                                    } else {
                                        SimpleActionDialogControlButtons(
                                            "Add",
                                            onLaunch = {
                                                singleLaunchGuard.launch { option.runAddExecution(markConfirmed) }
                                            },
                                            onClose = onClose,
                                            canLaunch = state == Execution.NotRunning && (!option.storageMarking.isRemark || markConfirmed == true),
                                        )
                                    }
                                }

                                is AddFileSystemRepositoryOperation.EncryptedFileSystemRepository -> {
                                    val state = option.addStatus.collectAsState().value

                                    if (state is Execution.Finished) {
                                        SimpleActionDialogDoneButtons(onClose)
                                    } else {
                                        val isUnlocked =
                                            option.unlockStatus.collectAsState().value.let {
                                                it is Execution.Finished &&
                                                    it.outcome is ExecutionOutcome.Success
                                            }

                                        SimpleActionDialogControlButtons(
                                            "Add",
                                            onLaunch = {
                                                singleLaunchGuard.launch { option.runAddExecution(markConfirmed) }
                                            },
                                            onClose = onClose,
                                            canLaunch =
                                                state == Execution.NotRunning && isUnlocked && (!option.storageMarking.isRemark || markConfirmed == true),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun StorageMark(
    storageMarkMatch: StorageMarking?,
    markConfirmed: Boolean?,
    setMarkConfirmed: (newValue: Boolean?) -> Unit,
) {
    when (storageMarkMatch) {
        StorageMarking.ALRIGHT -> {}

        StorageMarking.NEEDS_MARK_AS_LOCAL -> {
            ProgressText("Storage is used for the first time, and it will be marked as local.")
        }

        StorageMarking.NEEDS_MARK_AS_EXTERNAL -> {
            ProgressText("Storage is used for the first time, and it will be marked as external.")
        }

        StorageMarking.NEEDS_REMARK_AS_LOCAL -> {
            Text("Storage is currently external, re-mark it to local?")
            CheckboxWithText(
                markConfirmed == true,
                text = "Yes, re-mark storage as local",
                onValueChange = { setMarkConfirmed(it) },
            )
        }

        StorageMarking.NEEDS_REMARK_AS_EXTERNAL -> {
            ProgressText("Storage is currently local, re-mark it as external?")
            CheckboxWithText(
                markConfirmed == true,
                text = "Yes, re-mark storage as external",
                onValueChange = { setMarkConfirmed(it) },
            )
        }

        null -> {
            ProgressText("Preparing ...")
        }
    }
}

@Composable
private fun InitStatus(initStatus: Execution) {
    when (initStatus) {
        is Execution.Finished -> {
            when (val outcome = initStatus.outcome) {
                is ExecutionOutcome.Failed -> AutomaticErrorMessage(outcome, onResolve = {})
                is ExecutionOutcome.Success -> ProgressText("Directory initialized successfully as repository.")
            }
        }
        is Execution.InProgress -> ProgressText("Initializing...")
        Execution.NotRunning -> {}
    }
}

@Composable
private fun AddStatus(
    addStatus: Execution,
    notRunningStatus: @Composable () -> Unit = {},
) {
    when (addStatus) {
        is Execution.Finished -> {
            when (val outcome = addStatus.outcome) {
                is ExecutionOutcome.Failed -> AutomaticErrorMessage(outcome, onResolve = {})
                is ExecutionOutcome.Success -> ProgressText("Added successfully.")
            }
        }
        is Execution.InProgress -> ProgressText("Adding...")
        Execution.NotRunning -> notRunningStatus()
    }
}

@Composable
private fun ProgressText(text: String?) {
    if (text != null) {
        Text(text, modifier = Modifier.padding(top = 16.dp))
    }
}
