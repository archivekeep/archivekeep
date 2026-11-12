package org.archivekeep.app.ui.dialogs.storages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_forget
import org.archivekeep.ui.resources.dialog_forget_storage_data_not_deleted
import org.archivekeep.ui.resources.dialog_forget_storage_message
import org.archivekeep.ui.resources.dialog_forget_storage_title
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.jetbrains.compose.resources.stringResource

class ForgetStorageDialog(
    uri: StorageURI,
) : AbstractStorageDialog<ForgetStorageDialog.State, ForgetStorageDialog.VM>(uri) {
    class VM(
        val coroutineScope: CoroutineScope,
        val registry: StorageRegistry,
        val storage: Storage,
        val onClose: () -> Unit,
    ) {
        val launchable =
            simpleLaunchable(coroutineScope) {
                registry.forgetStorage(storage.uri)
                onClose()
            }
    }

    class State(
        val storage: KnownStorage,
        val launchable: Launchable<Unit>,
    ) {
        val action by launchable.asAction(
            onLaunch = { onLaunch(Unit) },
        )
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        storage: Storage,
        onClose: () -> Unit,
    ): VM {
        val registry = LocalApplicationServices.current.storageRegistry
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
            append(stringResource(Res.string.dialog_forget_storage_title))
        }
    }

    @Composable
    override fun renderContent(state: State) {
        Text(
                buildAnnotatedString {
                    append(stringResource(Res.string.dialog_forget_storage_message, state.storage.label))
                }
        )
        Text(
            stringResource(Res.string.dialog_forget_storage_data_not_deleted),
            modifier = Modifier.padding(top = 10.dp),
        )
        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(
        onClose: () -> Unit,
        state: State,
    ) {
        SimpleActionDialogControlButtons(
            stringResource(Res.string.common_forget),
            actionState = state.action,
            onClose = onClose,
        )
    }
}
