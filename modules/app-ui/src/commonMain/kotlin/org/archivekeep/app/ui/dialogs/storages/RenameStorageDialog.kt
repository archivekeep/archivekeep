package org.archivekeep.app.ui.dialogs.storages

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
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.ui.components.designsystem.input.TextField
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.domain.wiring.LocalRegistry
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData

class RenameStorageDialog(
    uri: StorageURI,
) : AbstractStorageDialog<RenameStorageDialog.State, RenameStorageDialog.VM>(uri) {
    class VM(
        val coroutineScope: CoroutineScope,
        val registry: RegistryDataStore,
        val storage: Storage,
        val onClose: () -> Unit,
    ) {
        var launchable =
            simpleLaunchable(coroutineScope) { newName: String ->
                registry.updateStorage(
                    storage.uri,
                ) {
                    it.copy(label = newName)
                }
                onClose()
            }
    }

    class State(
        val storage: KnownStorage,
        val launchable: Launchable<String>,
    ) : IState {
        val isNewName = storage.registeredStorage?.let { it.label != null } != true

        val initialValue = storage.registeredStorage?.label ?: ""

        var newName by mutableStateOf(initialValue)

        val action by launchable.asAction(
            onLaunch = { onLaunch(newName.trim()) },
            canLaunch = { newName.trim() != initialValue && newName.isNotBlank() },
        )

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
            VM(coroutineScope, registry, storage, onClose)
        }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> {
        val knownStorage = vm.storage.knownStorageFlow.collectLoadableFlow()

        return remember(knownStorage) {
            knownStorage.mapLoadedData {
                State(
                    it,
                    vm.launchable,
                )
            }
        }
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
        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(
        onClose: () -> Unit,
        state: State,
    ) {
        SimpleActionDialogControlButtons(
            "Submit",
            actionState = state.action,
            onClose = onClose,
        )
    }
}
