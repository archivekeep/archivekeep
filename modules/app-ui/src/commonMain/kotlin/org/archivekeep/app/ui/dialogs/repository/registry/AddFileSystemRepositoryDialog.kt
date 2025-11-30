package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.AddStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.InitStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.PreparationStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.StorageMarking
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryUseCase
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCard
import org.archivekeep.app.ui.components.designsystem.dialog.DialogFilePicker
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogInputLabel
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlay
import org.archivekeep.app.ui.components.designsystem.elements.WarningAlert
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogDoneButtons
import org.archivekeep.app.ui.components.feature.errors.AutomaticErrorMessage
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.LocalOperationFactory
import org.archivekeep.app.ui.domain.wiring.OperationFactory
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.filesystem.LocalFilesystemDirectoryPicker
import org.archivekeep.app.ui.utils.filesystem.PickResult

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

        var addOperation by mutableStateOf<AddFileSystemRepositoryOperation?>(null)
            private set

        fun setPick(result: PickResult) {
            pickResult = result
            markConfirmed = null

            addOperation?.run { cancel() }

            when (result) {
                is PickResult.Success -> {
                    val newOperation =
                        operationFactory
                            .get(AddFileSystemRepositoryUseCase::class.java)
                            .begin(
                                coroutineScope,
                                result.path,
                                intendedStorageType,
                            )

                    addOperation = newOperation
                }

                is PickResult.Failure -> {
                    addOperation = null
                }
            }
        }

        override fun onAbandoned() {
            addOperation?.run { cancel() }
        }

        override fun onForgotten() {
            addOperation?.run { cancel() }
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

        val addOperation = vm.addOperation

        val preparationStatus = addOperation?.preparationStatus?.collectAsState()?.value
        val initStatus = addOperation?.initStatus?.collectAsState()?.value
        val addStatus = addOperation?.addStatus?.collectAsState()?.value

        val permissionGrant = platformSpecificFileSystemRepositoryGuard()

        val onLaunch = LocalFilesystemDirectoryPicker.current(vm::setPick)

        LaunchedEffect(permissionGrant) {
            if (permissionGrant == PlatformSpecificPermissionFulfilment.IsFine) {
                onLaunch()
            }
        }

        if (addStatus is AddStatus.AddSuccessful) {
            // TODO: init storage as local or so
            // TODO: auto close: onClose()
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
                preparationStatus,
                initStatus,
                addStatus,
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
    preparationStatus: PreparationStatus?,
    initStatus: InitStatus?,
    addStatus: AddStatus?,
    onClose: () -> Unit,
) {
    val storageMarkMatch =
        preparationStatus?.let {
            when (it) {
                is PreparationStatus.ReadyForAdd -> it.storageMarking
                is PreparationStatus.ReadyForInit -> it.storageMarking
                else -> null
            }
        }

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
                            changeEnabled = initStatus == null && addStatus == null,
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

                ScrollableColumn {
                    PreparationStatus(preparationStatus, addStatus, initStatus, onClose)

                    if (
                        (preparationStatus is PreparationStatus.ReadyForAdd || preparationStatus is PreparationStatus.ReadyForInit) &&
                        initStatus == null &&
                        addStatus == null
                    ) {
                        when (storageMarkMatch) {
                            StorageMarking.ALRIGHT -> {
                                PreparationStatus(
                                    preparationStatus,
                                    addStatus,
                                    initStatus,
                                    onClose,
                                )
                            }

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
                    InitStatus(initStatus)
                    AddStatus(addStatus)
                }
            },
            bottomContent = {
                DialogButtonContainer {
                    val (name, canLaunch, onLaunch) =
                        when (preparationStatus) {
                            null, is PreparationStatus.PreparationNoContinue, PreparationStatus.Preparing -> {
                                Triple("Add", false, {})
                            }

                            is PreparationStatus.ReadyForAdd -> {
                                Triple(
                                    "Add",
                                    addStatus == null && (storageMarkMatch?.isRemark == false || markConfirmed == true),
                                    { preparationStatus.startAddExecution(markConfirmed) },
                                )
                            }

                            is PreparationStatus.ReadyForInit -> {
                                Triple(
                                    "Init",
                                    initStatus == null && (storageMarkMatch?.isRemark == false || markConfirmed == true),
                                    { preparationStatus.startInit(markConfirmed) },
                                )
                            }
                        }

                    if (addStatus == null && initStatus == null) {
                        SimpleActionDialogControlButtons(
                            name,
                            onLaunch = onLaunch,
                            onClose = onClose,
                            canLaunch = canLaunch,
                        )
                    } else {
                        SimpleActionDialogDoneButtons(onClose)
                    }
                }
            },
        )
    }
}

@Composable
private fun PreparationStatus(
    preparationStatus: PreparationStatus?,
    addStatus: AddStatus?,
    initStatus: InitStatus?,
    onClose: () -> Unit,
) {
    val l = LocalArchiveOperationLaunchers.current

    when (preparationStatus) {
        is PreparationStatus.PreparationException ->
            AutomaticErrorMessage(preparationStatus.cause, onResolve = {})

        is PreparationStatus.NotRoot -> {
            WarningAlert {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        buildAnnotatedString {
                            append("The parent directory ")
                            appendBoldSpan(preparationStatus.rootPath)
                            append(" is a archive repository.")
                        },
                    )
                    Text("Creation of repositories in a sub-directory of existing repository isn't supported.")
                    Text("Select a location, which is not inside of a repository.")

                    HorizontalDivider()

                    Text("Alternatively, deinitialize the existing parent directory to not be a repository.")

                    OutlinedButton(onClick = {
                        onClose()
                        l.openDeinitializeFilesystemRepository(preparationStatus.rootPath)
                    }) {
                        Text("Deinitialize existing repository")
                    }
                }
            }
        }

        is PreparationStatus.PreparationNoContinue -> ProgressText("Error $preparationStatus")

        PreparationStatus.Preparing -> ProgressText("Preparing...")

        is PreparationStatus.ReadyForAdd -> {
            if (addStatus == null) {
                ProgressText("Repository can be added.")
            }
        }

        is PreparationStatus.ReadyForInit ->
            if (initStatus == null) {
                ProgressText("The directory is not a repository, yet. Continue to initialize it as an archive repository.")
            }

        null -> {}
    }
}

@Composable
private fun InitStatus(initStatus: InitStatus?) {
    when (initStatus) {
        is InitStatus.InitFailed -> {
            AutomaticErrorMessage(initStatus.cause, onResolve = {})
        }

        InitStatus.InitSuccessful -> ProgressText("Directory initialized successfully as repository.")
        InitStatus.Initializing -> ProgressText("Initializing...")
        null -> {}
    }
}

@Composable
private fun AddStatus(addStatus: AddStatus?) {
    when (addStatus) {
        is AddStatus.AddFailed -> {
            AutomaticErrorMessage(addStatus.cause, onResolve = {})
        }

        AddStatus.AddSuccessful -> ProgressText("Added successfully.")
        AddStatus.Adding -> ProgressText("Adding...")
        null -> {}
    }
}

@Composable
private fun ProgressText(text: String?) {
    if (text != null) {
        Text(text, modifier = Modifier.padding(top = 16.dp))
    }
}
