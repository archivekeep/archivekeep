package org.archivekeep.app.ui.dialogs.storages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.domain.wiring.LocalRegistry
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.asTrivialAction
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData

class MarkAsLocalDialog(
    uri: StorageURI,
) : AbstractStorageDialog<MarkAsLocalDialog.State, MarkAsLocalDialog.VM>(uri) {
    class VM(
        val coroutineScope: CoroutineScope,
        val registry: RegistryDataStore,
        val storage: Storage,
        val onClose: () -> Unit,
    ) {
        val action =
            simpleLaunchable(coroutineScope) {
                registry.updateStorage(
                    storage.uri,
                ) {
                    it.copy(isLocal = true)
                }
                onClose()
            }
    }

    class State(
        val storage: KnownStorage,
        val launchable: Launchable<Unit>,
    ) : IState {
        override val title = buildAnnotatedString { append("Mark storage as local") }

        val action = launchable.asTrivialAction()
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
                    vm.action,
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
                    append("Mark storage ")
                    appendBoldSpan(state.storage.label)
                    append(" as local.")
                },
        )
        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(
        onClose: () -> Unit,
        state: State,
    ) {
        SimpleActionDialogControlButtons(
            "Mark as local",
            actionState = state.action.value,
            onClose = onClose,
        )
    }
}
