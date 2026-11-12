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
import androidx.compose.ui.text.AnnotatedString
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
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_submit
import org.archivekeep.ui.resources.dialog_rename_enter_name
import org.archivekeep.ui.resources.dialog_rename_new_name
import org.archivekeep.ui.resources.dialog_rename_storage_message
import org.archivekeep.ui.resources.dialog_rename_storage_title
import org.archivekeep.ui.resources.dialog_set_storage_name_message
import org.archivekeep.ui.resources.dialog_set_storage_name_title
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.jetbrains.compose.resources.stringResource

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
    ) {
        val isNewName = storage.registeredStorage?.let { it.label != null } != true

        val initialValue = storage.registeredStorage?.label ?: ""

        var newName by mutableStateOf(initialValue)

        val action by launchable.asAction(
            onLaunch = { onLaunch(newName.trim()) },
            canLaunch = { newName.trim() != initialValue && newName.isNotBlank() },
        )
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        storage: Storage,
        onClose: () -> Unit,
    ): VM {
        val registry = LocalApplicationServices.current.registry
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
    override fun title(state: State): AnnotatedString {
        return buildAnnotatedString {
            append(
                if (state.isNewName) {
                    stringResource(Res.string.dialog_set_storage_name_title)
                } else {
                    stringResource(Res.string.dialog_rename_storage_title)
                },
            )
        }
    }

    @Composable
    override fun renderContent(state: State) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text =
                buildAnnotatedString {
                    if (state.isNewName) {
                        append(stringResource(Res.string.dialog_set_storage_name_message, state.storage.label))
                    } else {
                        append(stringResource(Res.string.dialog_rename_storage_message, state.storage.label))
                    }
                },
        )
        TextField(
            state.newName,
            onValueChange = { state.newName = it },
            label = {
                Text(stringResource(Res.string.dialog_rename_new_name))
            },
            placeholder = {
                Text(stringResource(Res.string.dialog_rename_enter_name))
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
            stringResource(Res.string.common_submit),
            actionState = state.action,
            onClose = onClose,
        )
    }
}
