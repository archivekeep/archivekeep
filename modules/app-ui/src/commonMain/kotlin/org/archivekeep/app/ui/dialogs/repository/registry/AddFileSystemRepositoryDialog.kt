package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.add.AddFileSystemRepositoryOperation
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.add.AddFileSystemRepositoryUseCase
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.add.StorageRegistrationInput
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.add.StorageRegistrationState
import org.archivekeep.app.core.utils.generics.Execution
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCard
import org.archivekeep.app.ui.components.designsystem.dialog.DialogFilePicker
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInputLabel
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlay
import org.archivekeep.app.ui.components.designsystem.elements.InfoAlert
import org.archivekeep.app.ui.components.designsystem.elements.WarningAlert
import org.archivekeep.app.ui.components.designsystem.input.PasswordField
import org.archivekeep.app.ui.components.designsystem.input.RadioWithText
import org.archivekeep.app.ui.components.designsystem.input.RadioWithTextAndExtra
import org.archivekeep.app.ui.components.designsystem.input.TextField
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogDoneButtons
import org.archivekeep.app.ui.components.feature.errors.AutomaticErrorMessage
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.OperationFactory
import org.archivekeep.app.ui.utils.SingleLaunchGuard
import org.archivekeep.app.ui.utils.filesystem.LocalFilesystemDirectoryPicker
import org.archivekeep.app.ui.utils.filesystem.PickResult
import org.archivekeep.files.driver.filesystem.files.PlainRepositoryType
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_add
import org.archivekeep.ui.resources.common_added_successfully
import org.archivekeep.ui.resources.common_adding
import org.archivekeep.ui.resources.common_enter_password
import org.archivekeep.ui.resources.common_error
import org.archivekeep.ui.resources.common_init
import org.archivekeep.ui.resources.common_initializing
import org.archivekeep.ui.resources.common_preparing
import org.archivekeep.ui.resources.common_unlock
import org.archivekeep.ui.resources.common_unlocking
import org.archivekeep.ui.resources.common_verify_password
import org.archivekeep.ui.resources.dialog_add_repo_can_be_added
import org.archivekeep.ui.resources.dialog_add_repo_checksum_files
import org.archivekeep.ui.resources.dialog_add_repo_checksum_warning
import org.archivekeep.ui.resources.dialog_add_repo_deinitialize_alternative
import org.archivekeep.ui.resources.dialog_add_repo_deinitialize_button
import org.archivekeep.ui.resources.dialog_add_repo_directory_initialized
import org.archivekeep.ui.resources.dialog_add_repo_directory_label
import org.archivekeep.ui.resources.dialog_add_repo_encrypted
import org.archivekeep.ui.resources.dialog_add_repo_encrypted_password_protected
import org.archivekeep.ui.resources.dialog_add_repo_enter_device_label
import org.archivekeep.ui.resources.dialog_add_repo_enter_label
import org.archivekeep.ui.resources.dialog_add_repo_enter_password_access
import org.archivekeep.ui.resources.dialog_add_repo_not_repo_yet
import org.archivekeep.ui.resources.dialog_add_repo_parent_is_repo
import org.archivekeep.ui.resources.dialog_add_repo_plain_files
import org.archivekeep.ui.resources.dialog_add_repo_rocksdb
import org.archivekeep.ui.resources.dialog_add_repo_rocksdb_info
import org.archivekeep.ui.resources.dialog_add_repo_select_directory
import org.archivekeep.ui.resources.dialog_add_repo_select_location
import org.archivekeep.ui.resources.dialog_add_repo_select_storage_type
import org.archivekeep.ui.resources.dialog_add_repo_sqlite
import org.archivekeep.ui.resources.dialog_add_repo_sqlite_warning
import org.archivekeep.ui.resources.dialog_add_repo_storage_external
import org.archivekeep.ui.resources.dialog_add_repo_storage_first_time
import org.archivekeep.ui.resources.dialog_add_repo_storage_local
import org.archivekeep.ui.resources.dialog_add_repo_subdir_not_supported
import org.archivekeep.ui.resources.dialog_add_repo_successfully_unlocked
import org.archivekeep.ui.resources.dialog_add_repo_title
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.resources.stringResource

class AddFileSystemRepositoryDialog : Dialog {
    class VM(
        val coroutineScope: CoroutineScope,
        val operationFactory: OperationFactory,
    ) : RememberObserver {
        var pickResult by mutableStateOf<PickResult?>(null)
            private set

        var storageType by mutableStateOf<FileSystemStorageType?>(null)
        var storageLabel by mutableStateOf<String>("")

        private var operationScope: CoroutineScope = coroutineScope + SupervisorJob()

        var addOperation by mutableStateOf<Loadable<AddFileSystemRepositoryOperation>?>(null)
            private set

        fun setPick(result: PickResult) {
            pickResult = result
            storageType = null
            storageLabel = ""

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
                                    .create(
                                        operationScope,
                                        result.path,
                                    )

                            ensureActive()
                            if (newOperation is AddFileSystemRepositoryOperation.Available) {
                                val registration = newOperation.storageRegistration
                                if (registration is StorageRegistrationState.Unregistered) {
                                    storageType = registration.suggestedType
                                    storageLabel = registration.suggestedLabel ?: ""
                                }
                            }
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
        val operationFactory = LocalApplicationServices.current.operationFactory

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
                permissionGrant,
                selectedPath,
                onLaunch,
                vm.storageType,
                { vm.storageType = it },
                vm.storageLabel,
                { vm.storageLabel = it },
                vm.addOperation,
                onClose,
                vm.storageType?.let { StorageRegistrationInput(it, vm.storageLabel) },
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
    permissionFulfilment: PlatformSpecificPermissionFulfilment,
    pick: PickResult?,
    onTriggerChange: () -> Unit,
    storageType: FileSystemStorageType?,
    setStorageType: (newValue: FileSystemStorageType) -> Unit,
    label: String,
    setLabel: (newValue: String) -> Unit,
    optionLoadable: Loadable<AddFileSystemRepositoryOperation>?,
    onClose: () -> Unit,
    storageRegistrationInput: StorageRegistrationInput?,
) {
    var createEncrypted by remember { mutableStateOf(false) }
    var createPassword by remember { mutableStateOf("") }
    var createPassword2 by remember { mutableStateOf("") }
    var createRepoType by remember { mutableStateOf(PlainRepositoryType.ROCKS_DB) }

    var password by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val passwordLaunchGuard = remember(coroutineScope) { SingleLaunchGuard(coroutineScope) }
    val singleLaunchGuard = remember(coroutineScope) { SingleLaunchGuard(coroutineScope) }

    val l = LocalArchiveOperationLaunchers.current

    DialogCard {
        DialogInnerContainer(
             buildAnnotatedString { append(stringResource(Res.string.dialog_add_repo_title)) } ,
            content = {
                when (permissionFulfilment) {
                    PlatformSpecificPermissionFulfilment.IsFine -> {
                        DialogInputLabel(stringResource(Res.string.dialog_add_repo_directory_label))

                        DialogFilePicker(
                            pick?.let {
                                when (pick) {
                                    is PickResult.Failure -> stringResource(Res.string.common_error, "")
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
                            ProgressText(stringResource(Res.string.common_preparing))
                        },
                    ) { option ->
                        when (option) {
                            is AddFileSystemRepositoryOperation.Invalid.NotRoot -> {
                                WarningAlert {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            buildAnnotatedString {
                                                append(stringResource(Res.string.dialog_add_repo_parent_is_repo, option.rootPath))
                                            },
                                        )
                                        Text(stringResource(Res.string.dialog_add_repo_subdir_not_supported))
                                        Text(stringResource(Res.string.dialog_add_repo_select_location))

                                        HorizontalDivider()

                                        Text(stringResource(Res.string.dialog_add_repo_deinitialize_alternative))

                                        OutlinedButton(onClick = {
                                            onClose()
                                            l.openDeinitializeFilesystemRepositoryByPath(option.rootPath)
                                        }) {
                                            Text(stringResource(Res.string.dialog_add_repo_deinitialize_button))
                                        }
                                    }
                                }
                            }

                            is AddFileSystemRepositoryOperation.Invalid -> {
                                ProgressText(stringResource(Res.string.common_error, option.toString()))
                            }

                            is AddFileSystemRepositoryOperation.DirectoryNotRepository -> {
                                val initStatus = option.initStatus.collectAsState().value
                                val addStatus = option.addStatus.collectAsState().value

                                val canEdit = option.initStatus.collectAsState().value == Execution.NotRunning

                                ProgressText(stringResource(Res.string.dialog_add_repo_not_repo_yet))

                                Spacer(Modifier.height(12.dp))
                                RadioWithTextAndExtra(
                                    selected = !createEncrypted,
                                    onClick = { createEncrypted = false },
                                    text = stringResource(Res.string.dialog_add_repo_plain_files),
                                    enabled = canEdit,
                                    extra = {
                                        Column {
                                            RadioWithTextAndExtra(
                                                selected = createRepoType == PlainRepositoryType.ROCKS_DB,
                                                onClick = { createRepoType = PlainRepositoryType.ROCKS_DB },
                                                text = stringResource(Res.string.dialog_add_repo_rocksdb),
                                                enabled = canEdit,
                                            ) {
                                                Column {
                                                    InfoAlert {
                                                        Text(stringResource(Res.string.dialog_add_repo_rocksdb_info))
                                                    }
                                                    Spacer(Modifier.height(8.dp))
                                                }
                                            }
                                            RadioWithTextAndExtra(
                                                selected = createRepoType == PlainRepositoryType.SQLITE,
                                                onClick = { createRepoType = PlainRepositoryType.SQLITE },
                                                text = stringResource(Res.string.dialog_add_repo_sqlite),
                                                enabled = canEdit,
                                            ) {
                                                Column {
                                                    WarningAlert {
                                                        Text(stringResource(Res.string.dialog_add_repo_sqlite_warning))
                                                    }
                                                    Spacer(Modifier.height(8.dp))
                                                }
                                            }
                                            RadioWithTextAndExtra(
                                                selected = createRepoType == PlainRepositoryType.CHECKSUM_FILES,
                                                onClick = { createRepoType = PlainRepositoryType.CHECKSUM_FILES },
                                                text = stringResource(Res.string.dialog_add_repo_checksum_files),
                                                enabled = canEdit,
                                            ) {
                                                Column {
                                                    WarningAlert {
                                                        Text(stringResource(Res.string.dialog_add_repo_checksum_warning))
                                                    }
                                                    Spacer(Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    },
                                )
                                RadioWithTextAndExtra(
                                    selected = createEncrypted,
                                    onClick = { createEncrypted = true },
                                    text = stringResource(Res.string.dialog_add_repo_encrypted),
                                    enabled = canEdit,
                                    extra = {
                                        Column {
                                            PasswordField(
                                                createPassword,
                                                onValueChange = { createPassword = it },
                                                placeholder = { Text(stringResource(Res.string.common_enter_password)) },
                                                modifier = Modifier.padding(bottom = 8.dp).testTag("Enter password …"),
                                                enabled = canEdit,
                                            )

                                            PasswordField(
                                                createPassword2,
                                                onValueChange = { createPassword2 = it },
                                                placeholder = { Text(stringResource(Res.string.common_verify_password)) },
                                                modifier = Modifier.padding(bottom = 8.dp).testTag("Verify password …"),
                                                enabled = canEdit,
                                            )
                                        }
                                    },
                                )

                                InitStatus(option.initStatus.collectAsState().value)
                                AddStatus(addStatus)

                                if (initStatus == Execution.NotRunning && addStatus == Execution.NotRunning) {
                                    StorageMark(option.storageRegistration, storageType, setStorageType, label, setLabel)
                                }
                            }

                            is AddFileSystemRepositoryOperation.PlainFileSystemRepository -> {
                                val addStatus = option.addStatus.collectAsState().value

                                AddStatus(addStatus, notRunningStatus = { ProgressText(stringResource(Res.string.dialog_add_repo_can_be_added)) })

                                if (addStatus == Execution.NotRunning) {
                                    StorageMark(option.storageRegistration, storageType, setStorageType, label, setLabel)
                                }
                            }

                            is AddFileSystemRepositoryOperation.EncryptedFileSystemRepository -> {
                                val addStatus = option.addStatus.collectAsState().value

                                ProgressText(stringResource(Res.string.dialog_add_repo_encrypted_password_protected))

                                option.unlockStatus.collectAsState().value.let {
                                    if (it is Execution.InProgress) {
                                        ProgressText(stringResource(Res.string.common_unlocking))
                                    } else if (it is Execution.Finished && it.outcome is ExecutionOutcome.Success) {
                                        ProgressText(stringResource(Res.string.dialog_add_repo_successfully_unlocked))
                                    } else {
                                        Text(
                                            stringResource(Res.string.dialog_add_repo_enter_password_access),
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                        )
                                        PasswordField(
                                            password,
                                            onValueChange = { password = it },
                                            placeholder = { Text(stringResource(Res.string.common_enter_password)) },
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        )
                                        Button(
                                            onClick = { passwordLaunchGuard.launch { option.unlock(password) } },
                                            enabled = password.isNotEmpty(),
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.padding(bottom = 8.dp),
                                        ) {
                                            Text(stringResource(Res.string.common_unlock))
                                        }

                                        passwordLaunchGuard.executionOutcome.value.let {
                                            if (it is ExecutionOutcome.Failed) {
                                                AutomaticErrorMessage(it.cause, onResolve = { singleLaunchGuard.reset() })
                                            }
                                        }
                                    }
                                }

                                if (addStatus == Execution.NotRunning) {
                                    StorageMark(option.storageRegistration, storageType, setStorageType, label, setLabel)
                                }

                                AddStatus(addStatus)
                            }
                        }
                    }
                } else {
                    Text(stringResource(Res.string.dialog_add_repo_select_directory))
                }
            },
            bottomContent = {
                DialogButtonContainer {
                    when (optionLoadable) {
                        is Loadable.Failed, null, Loadable.Loading -> {
                            SimpleActionDialogControlButtons(
                                stringResource(Res.string.common_add),
                                onLaunch = {},
                                onClose = onClose,
                                canLaunch = false,
                            )
                        }

                        is Loadable.Loaded<*> -> {
                            when (val option = optionLoadable.value) {
                                is AddFileSystemRepositoryOperation.Invalid -> {
                                    SimpleActionDialogControlButtons(
                                        stringResource(Res.string.common_add),
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
                                            stringResource(Res.string.common_init),
                                            onLaunch = {
                                                if (!createEncrypted) {
                                                    singleLaunchGuard.launch {
                                                        option.startInitAsPlain(storageRegistrationInput, createRepoType)
                                                    }
                                                } else {
                                                    singleLaunchGuard.launch {
                                                        option.startInitAsEncrypted(storageRegistrationInput, createPassword)
                                                    }
                                                }
                                            },
                                            onClose = onClose,
                                            canLaunch =
                                                state == Execution.NotRunning &&
                                                    (
                                                        option.storageRegistration is StorageRegistrationState.Registered ||
                                                            (storageType != null && label.isNotBlank())
                                                    ) &&

                                                    (
                                                        !createEncrypted ||
                                                            (
                                                                createPassword.isNotBlank() && createPassword == createPassword2
                                                            )
                                                    ),
                                            isRunning = singleLaunchGuard.state is SingleLaunchGuard.State.Running,
                                        )
                                    }
                                }

                                is AddFileSystemRepositoryOperation.PlainFileSystemRepository -> {
                                    val state = option.addStatus.collectAsState().value

                                    if (state is Execution.Finished) {
                                        SimpleActionDialogDoneButtons(onClose)
                                    } else {
                                        SimpleActionDialogControlButtons(
                                            stringResource(Res.string.common_add),
                                            onLaunch = {
                                                singleLaunchGuard.launch { option.executeAdd(storageRegistrationInput) }
                                            },
                                            onClose = onClose,
                                            canLaunch =
                                                state == Execution.NotRunning &&
                                                    (
                                                        option.storageRegistration is StorageRegistrationState.Registered ||
                                                            (storageType != null && label.isNotBlank())
                                                    ),
                                            isRunning = singleLaunchGuard.state is SingleLaunchGuard.State.Running,
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
                                            stringResource(Res.string.common_add),
                                            onLaunch = {
                                                singleLaunchGuard.launch { option.executeAdd(storageRegistrationInput) }
                                            },
                                            onClose = onClose,
                                            canLaunch =
                                                state == Execution.NotRunning && isUnlocked &&
                                                    (
                                                        option.storageRegistration is StorageRegistrationState.Registered ||
                                                            (storageType != null && label.isNotBlank())
                                                    ),
                                            isRunning = singleLaunchGuard.state is SingleLaunchGuard.State.Running,
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
    storageRegistrationState: StorageRegistrationState,
    storageType: FileSystemStorageType?,
    setStorageType: (newValue: FileSystemStorageType) -> Unit,
    label: String,
    setLabel: (newValue: String) -> Unit,
) {
    if (storageRegistrationState is StorageRegistrationState.Unregistered) {
        Spacer(Modifier.height(12.dp))
        InfoAlert {
            Text(stringResource(Res.string.dialog_add_repo_storage_first_time), Modifier.padding(bottom = 16.dp))
            Text(stringResource(Res.string.dialog_add_repo_select_storage_type), Modifier.padding(bottom = 4.dp))
            RadioWithText(
                storageType == FileSystemStorageType.LOCAL,
                onClick = { setStorageType(FileSystemStorageType.LOCAL) },
                text = stringResource(Res.string.dialog_add_repo_storage_local),
            )
            RadioWithText(
                storageType == FileSystemStorageType.EXTERNAL,
                onClick = { setStorageType(FileSystemStorageType.EXTERNAL) },
                text = stringResource(Res.string.dialog_add_repo_storage_external),
            )
            Text(stringResource(Res.string.dialog_add_repo_enter_label), Modifier.padding(top = 12.dp))
            TextField(
                value = label,
                onValueChange = { setLabel(it) },
                placeholder = { Text(stringResource(Res.string.dialog_add_repo_enter_device_label)) },
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun InitStatus(initStatus: Execution) {
    when (initStatus) {
        is Execution.Finished -> {
            when (val outcome = initStatus.outcome) {
                is ExecutionOutcome.Failed -> AutomaticErrorMessage(outcome, onResolve = {})
                is ExecutionOutcome.Success -> ProgressText(stringResource(Res.string.dialog_add_repo_directory_initialized))
            }
        }

        is Execution.InProgress -> {
            ProgressText(stringResource(Res.string.common_initializing))
        }

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
                is ExecutionOutcome.Success -> ProgressText(stringResource(Res.string.common_added_successfully))
            }
        }

        is Execution.InProgress -> {
            ProgressText(stringResource(Res.string.common_adding))
        }

        Execution.NotRunning -> {
            notRunningStatus()
        }
    }
}

@Composable
private fun ProgressText(text: String?) {
    if (text != null) {
        Text(text, modifier = Modifier.padding(top = 16.dp))
    }
}
