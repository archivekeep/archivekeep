package org.archivekeep.app.desktop.ui.dialogs.repository.registry

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
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
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.core.FileKitPlatformSettings
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.AddStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.InitStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.PreparationStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.StorageMarking
import org.archivekeep.app.desktop.domain.wiring.LocalComposeWindow
import org.archivekeep.app.desktop.domain.wiring.LocalOperationFactory
import org.archivekeep.app.desktop.domain.wiring.OperationFactory
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogFilePicker
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInputLabel
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlay
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.designsystem.input.CheckboxWithText
import org.archivekeep.app.desktop.ui.dialogs.Dialog

class AddFileSystemRepositoryDialog(
    val intendedStorageType: FileSystemStorageType?,
) : Dialog {
    inner class VM(
        val coroutineScope: CoroutineScope,
        val operationFactory: OperationFactory,
    ) : RememberObserver {
        var selectedPath by mutableStateOf<String?>(null)
            private set

        var markConfirmed by mutableStateOf<Boolean?>(false)

        var addOperation by mutableStateOf<AddFileSystemRepositoryOperation?>(null)
            private set

        fun ssetSelectedPath(path: String) {
            selectedPath = path
            markConfirmed = null

            addOperation?.run { cancel() }

            val newOperation =
                operationFactory
                    .get(AddFileSystemRepositoryOperation.Factory::class.java)
                    .create(
                        coroutineScope,
                        path,
                        intendedStorageType,
                    )

            addOperation = newOperation
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

        val launcher =
            rememberDirectoryPickerLauncher(
                title = "Pick a directory",
                platformSettings =
                    FileKitPlatformSettings(
                        parentWindow = LocalComposeWindow.current,
                    ),
            ) { directory ->
                if (directory != null) {
                    val path = directory.path ?: throw Error("Path not present for $directory")

                    vm.ssetSelectedPath(path)
                }
            }

        LaunchedEffect(0) {
            launcher.launch()
        }

        if (addStatus is AddStatus.AddSuccessful) {
            // TODO: init storage as local or so
            // TODO: auto close: onClose()
        }
        val selectedPath = vm.selectedPath
        val onTriggerChange = launcher::launch

        DialogOverlay(onDismissRequest = onClose) {
            AddRepositoryDialogContents(
                intendedStorageType,
                selectedPath,
                onTriggerChange,
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

@Composable
private fun AddRepositoryDialogContents(
    intendedStorageType: FileSystemStorageType?,
    selectedPath: String?,
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
                DialogInputLabel("Repository directory:")

                DialogFilePicker(
                    selectedPath,
                    onTriggerChange = onTriggerChange,
                    changeEnabled = initStatus == null && addStatus == null,
                )

                if (preparationStatus is PreparationStatus.ReadyForAdd || preparationStatus is PreparationStatus.ReadyForInit) {
                    when (storageMarkMatch) {
                        StorageMarking.ALRIGHT -> {
                            ProgressText(
                                preparationStatusText(
                                    preparationStatus,
                                    addStatus,
                                    initStatus,
                                ),
                            )
                        }
                        StorageMarking.NEEDS_MARK_AS_LOCAL -> {
                            Text("Storage will be marked as local")
                        }
                        StorageMarking.NEEDS_MARK_AS_EXTERNAL -> {
                            Text("Storage will be marked as external")
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
                            Text("Storage is currently local, re-mark it as external?")
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
                } else {
                    ProgressText(preparationStatusText(preparationStatus, addStatus, initStatus))
                }
                ProgressText(initStatusText(initStatus))
                ProgressText(addStatusText(addStatus))
            },
            bottomContent = {
                DialogButtonContainer {
                    when (preparationStatus) {
                        null, is PreparationStatus.PreparationNoContinue, PreparationStatus.Preparing -> {
                            DialogPrimaryButton(
                                "Add",
                                enabled = false,
                                onClick = {},
                            )
                        }

                        is PreparationStatus.ReadyForAdd -> {
                            DialogPrimaryButton(
                                "Add",
                                enabled = addStatus == null && (storageMarkMatch?.isRemark == false || markConfirmed == true),
                                onClick = {
                                    preparationStatus.startAddExecution(markConfirmed)
                                },
                            )
                        }

                        is PreparationStatus.ReadyForInit -> {
                            DialogPrimaryButton(
                                "Init",
                                enabled = initStatus == null && (storageMarkMatch?.isRemark == false || markConfirmed == true),
                                onClick = {
                                    preparationStatus.startInit(markConfirmed)
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    DialogDismissButton(
                        "Dismiss",
                        onClick = onClose,
                        enabled = true,
                    )
                }
            },
        )
    }
}

private fun preparationStatusText(
    preparationStatus: PreparationStatus?,
    addStatus: AddStatus?,
    initStatus: InitStatus?,
) = when (preparationStatus) {
    is PreparationStatus.PreparationNoContinue -> "Error $preparationStatus"

    PreparationStatus.Preparing -> "Preparing..."

    is PreparationStatus.ReadyForAdd -> {
        if (addStatus == null) {
            "Repository can be added."
        } else {
            null
        }
    }

    is PreparationStatus.ReadyForInit ->
        if (initStatus == null) {
            "The directory is not a repository, yet. Continue to initialize it as an archive repository."
        } else {
            null
        }

    null -> null
}

private fun initStatusText(initStatus: InitStatus?) =
    when (initStatus) {
        is InitStatus.InitFailed -> "Error: $initStatus"
        InitStatus.InitSuccessful -> "Initialized successfully."
        InitStatus.Initializing -> "Initializing..."
        null -> null
    }

private fun addStatusText(addStatus: AddStatus?) =
    when (addStatus) {
        is AddStatus.AddFailed -> "Error: $addStatus"
        AddStatus.AddSuccessful -> "Added successfully."
        AddStatus.Adding -> "Adding..."
        null -> null
    }

@Composable
private fun ProgressText(text: String?) {
    if (text != null) {
        Text(text, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
@Preview
private fun AddRepositoryDialogPreview1() {
    DialogPreviewColumn {
        AddRepositoryDialogContents(
            null,
            null,
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = null,
            initStatus = null,
            addStatus = null,
            onClose = {},
        )
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.Preparing,
            initStatus = null,
            addStatus = null,
            onClose = {},
        )
    }
}

@Composable
@Preview
private fun AddRepositoryDialogPreview2() {
    DialogPreviewColumn {
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.ReadyForAdd(startAddExecution = {}, StorageMarking.ALRIGHT),
            initStatus = null,
            addStatus = null,
            onClose = {},
        )
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.ReadyForInit(startInit = {}, StorageMarking.ALRIGHT),
            initStatus = null,
            addStatus = null,
            onClose = {},
        )
    }
}

@Composable
@Preview
private fun AddRepositoryDialogPreview3() {
    DialogPreviewColumn {
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.ReadyForAdd(startAddExecution = {}, StorageMarking.ALRIGHT),
            initStatus = null,
            addStatus = AddStatus.Adding,
            onClose = {},
        )
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.ReadyForAdd(startAddExecution = {}, StorageMarking.ALRIGHT),
            initStatus = null,
            addStatus = AddStatus.AddSuccessful,
            onClose = {},
        )
    }
}

@Composable
@Preview
private fun AddRepositoryDialogPreview4() {
    DialogPreviewColumn {
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.ReadyForInit(startInit = {}, StorageMarking.ALRIGHT),
            initStatus = InitStatus.Initializing,
            addStatus = null,
            onClose = {},
        )
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.ReadyForInit(startInit = {}, StorageMarking.ALRIGHT),
            initStatus = InitStatus.InitSuccessful,
            addStatus = AddStatus.Adding,
            onClose = {},
        )
    }
}

@Composable
@Preview
private fun AddRepositoryDialogPreview5() {
    DialogPreviewColumn {
        AddRepositoryDialogContents(
            null,
            "/home/you/Archive/PersonalStuff",
            onTriggerChange = {},
            markConfirmed = false,
            setMarkConfirmed = {},
            preparationStatus = PreparationStatus.ReadyForInit(startInit = {}, StorageMarking.ALRIGHT),
            initStatus = InitStatus.InitSuccessful,
            addStatus = AddStatus.AddSuccessful,
            onClose = {},
        )
    }
}
