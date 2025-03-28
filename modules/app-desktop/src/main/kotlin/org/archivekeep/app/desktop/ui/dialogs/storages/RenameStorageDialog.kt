package org.archivekeep.app.desktop.ui.dialogs.storages

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.persistence.platform.demo.asKnownRegisteredStorage
import org.archivekeep.app.core.persistence.platform.demo.asKnownStorage
import org.archivekeep.app.core.persistence.platform.demo.hddA
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.desktop.domain.wiring.LocalRegistry
import org.archivekeep.app.desktop.ui.components.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.input.TextField
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.utils.loading.Loadable

class RenameStorageDialog(
    uri: StorageURI,
) : AbstractStorageDialog<RenameStorageDialog.State, RenameStorageDialog.VM>(uri) {
    class VM(
        val coroutineScope: CoroutineScope,
        val registry: RegistryDataStore,
        val storage: KnownStorage,
        val onClose: () -> Unit,
    ) {
        var runningJob by mutableStateOf<Job?>(null)

        fun launch(newName: String) {
            runningJob =
                coroutineScope.launch {
                    registry.updateStorage(
                        storage.storageURI,
                    ) {
                        it.copy(label = newName)
                    }
                    onClose()
                }
        }
    }

    class State(
        val storage: KnownStorage,
        val onLaunch: (newName: String) -> Unit,
    ) : IState {
        val isNewName = storage.registeredStorage?.let { it.label != null } != true

        val initialValue = storage.registeredStorage?.label ?: ""

        var newName by mutableStateOf(initialValue)

        val canSubmit: Boolean
            @Composable
            get() = newName.trim() != initialValue && newName.isNotBlank()

        override val title =
            buildAnnotatedString {
                append(
                    if (isNewName) {
                        "Set name for storage"
                    } else {
                        "Rename storage"
                    },
                )
            }

        fun launch() {
            onLaunch(newName.trim())
        }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        storage: Storage,
        onClose: () -> Unit,
    ): VM {
        val registry = LocalRegistry.current
        val coroutineScope = rememberCoroutineScope()

        return remember {
            VM(coroutineScope, registry, storage.knownStorage, onClose)
        }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember {
            Loadable.Loaded(
                State(
                    vm.storage,
                    vm::launch,
                ),
            )
        }

    @Composable
    override fun renderContent(state: State) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text =
                buildAnnotatedString {
                    append("Storage ")
                    appendBoldSpan(state.storage.label)

                    if (state.isNewName) {
                        append(" will be named as:")
                    } else {
                        append(" will be renamed to new name:")
                    }
                },
        )
        TextField(
            state.newName,
            onValueChange = { state.newName = it },
            label = {
                Text("New name")
            },
            placeholder = {
                Text("Enter name")
            },
            singleLine = true,
        )
    }

    @Composable
    override fun RowScope.renderButtons(
        onClose: () -> Unit,
        state: State,
    ) {
        SimpleActionDialogControlButtons(
            "Submit",
            onLaunch = state::launch,
            onClose = onClose,
            canLaunch = state.canSubmit,
        )
    }
}

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        val dialog = RenameStorageDialog(hddA.uri)

        dialog.renderDialogCardForPreview(
            RenameStorageDialog.State(
                hddA.asKnownStorage(),
                onLaunch = {},
            ),
        )
    }
}

@Preview
@Composable
private fun preview2() {
    DialogPreviewColumn {
        val dialog = RenameStorageDialog(hddA.uri)

        dialog.renderDialogCardForPreview(
            RenameStorageDialog.State(
                hddA.asKnownRegisteredStorage(),
                onLaunch = {},
            ),
        )
    }
}
