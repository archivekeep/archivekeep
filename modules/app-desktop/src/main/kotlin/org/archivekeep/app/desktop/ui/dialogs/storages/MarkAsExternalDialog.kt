package org.archivekeep.app.desktop.ui.dialogs.storages

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.persistence.platform.demo.asKnownStorage
import org.archivekeep.app.core.persistence.platform.demo.hddA
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.desktop.domain.wiring.LocalRegistry
import org.archivekeep.app.desktop.ui.components.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.desktop.ui.components.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.utils.Launchable
import org.archivekeep.app.desktop.utils.asAction
import org.archivekeep.app.desktop.utils.mockLaunchable
import org.archivekeep.app.desktop.utils.simpleLaunchable
import org.archivekeep.utils.loading.Loadable

class MarkAsExternalDialog(
    uri: StorageURI,
) : AbstractStorageDialog<MarkAsExternalDialog.State, MarkAsExternalDialog.VM>(uri) {
    class VM(
        val coroutineScope: CoroutineScope,
        val registry: RegistryDataStore,
        val storage: KnownStorage,
        val onClose: () -> Unit,
    ) {
        val launchable =
            simpleLaunchable(coroutineScope) {
                registry.updateStorage(
                    storage.storageURI,
                ) {
                    it.copy(
                        isLocal = false,
                    )
                }
                onClose()
            }
    }

    class State(
        val storage: KnownStorage,
        val launchable: Launchable<Unit>,
    ) : IState {
        override val title =
            buildAnnotatedString {
                append("Mark storage as external")
            }

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
                    vm.launchable,
                ),
            )
        }

    @Composable
    override fun renderContent(state: State) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text =
                buildAnnotatedString {
                    append("Mark storage ")
                    appendBoldSpan(state.storage.label)
                    append(" as external")
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
            "Mark as external",
            actionState = state.action,
            onClose = onClose,
        )
    }
}

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        val dialog = MarkAsExternalDialog(hddA.uri)

        dialog.renderDialogCardForPreview(
            MarkAsExternalDialog.State(
                hddA.asKnownStorage(),
                mockLaunchable(false, null),
            ),
        )
    }
}
