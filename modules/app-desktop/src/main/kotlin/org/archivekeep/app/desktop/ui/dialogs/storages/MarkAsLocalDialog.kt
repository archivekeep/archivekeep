package org.archivekeep.app.desktop.ui.dialogs.storages

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import org.archivekeep.app.core.persistence.platform.demo.asKnownStorage
import org.archivekeep.app.core.persistence.platform.demo.hddA
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.desktop.domain.wiring.LocalRegistry
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.utils.loading.Loadable

class MarkAsLocalDialog(
    uri: StorageURI,
) : AbstractStorageDialog<MarkAsLocalDialog.State, MarkAsLocalDialog.VM>(uri) {
    class VM(
        val coroutineScope: CoroutineScope,
        val registry: RegistryDataStore,
        val storage: KnownStorage,
        val onClose: () -> Unit,
    ) {
        var runningJob by mutableStateOf<Job?>(null)

        fun launch() {
            runningJob =
                coroutineScope.launch {
                    registry.updateStorage(
                        storage.storageURI,
                    ) {
                        it.copy(
                            isLocal = true,
                        )
                    }
                    onClose()
                }
        }
    }

    class State(
        val storage: KnownStorage,
        val onLaunch: () -> Unit,
    ) : IState {
        override val title = buildAnnotatedString { append("Mark storage as local") }
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
                    append("Mark storage ")
                    appendBoldSpan(state.storage.label)
                    append(" as local.")
                },
        )
    }

    @Composable
    override fun RowScope.renderButtons(
        onClose: () -> Unit,
        state: State,
    ) {
        DialogPrimaryButton(
            "Mark as local",
            onClick = state.onLaunch,
        )
        Spacer(Modifier.weight(1f))
        DialogDismissButton("Cancel", onClick = onClose)
    }
}

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        val dialog = MarkAsLocalDialog(hddA.uri)

        dialog.renderDialogCardForPreview(
            MarkAsLocalDialog.State(
                hddA.asKnownStorage(),
                onLaunch = {},
            ),
        )
    }
}
