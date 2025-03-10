package org.archivekeep.app.desktop.ui.dialogs.repository.registry

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInHDDA
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRegistry
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable

class ForgetRepositoryDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<ForgetRepositoryDialog.State, ForgetRepositoryDialog.VM>(uri) {
    class VM(
        val scope: CoroutineScope,
        storageService: StorageService,
        val registry: RegistryDataStore,
        val uri: RepositoryURI,
        val _onClose: () -> Unit,
    ) : IVM {
        val storageRepository = storageService.repository(uri)

        val runningJob = mutableStateOf<Job?>(null)

        fun launch() {
            runningJob.value =
                scope.launch {
                    registry.updateRepositories { old ->
                        old.filter { it.uri != this@VM.uri }.toSet()
                    }
                    onClose()
                }
        }

        override fun onClose() {
            _onClose()
        }
    }

    data class State(
        val currentRepo: StorageRepository,
        val onLaunch: () -> Unit,
        val onClose: () -> Unit,
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Forget repository")
            }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): VM {
        val storageService = LocalStorageService.current
        val registry = LocalRegistry.current

        return remember {
            VM(scope, storageService, registry, uri, onClose)
        }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember(vm) {
            vm.storageRepository.mapToLoadable { storageRepository ->
                State(storageRepository, vm::launch, vm::onClose)
            }
        }.collectLoadableFlow()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        Text(
            remember(state.currentRepo) {
                buildAnnotatedString {
                    append("Remove repository ")
                    appendBoldSpan(state.currentRepo.repositoryState.displayName)
                    append(" stored in ")
                    appendBoldSpan(state.currentRepo.storage.displayName)
                    append(" from local registry of known repositories.")
                }
            },
        )
        Text(
            remember(state.currentRepo) {
                buildAnnotatedString {
                    append("Data will not be deleted.")
                }
            },
            modifier = Modifier.padding(top = 10.dp),
        )
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogPrimaryButton(
            "Forget",
            onClick = state.onLaunch,
            enabled = true,
        )

        Spacer(modifier = Modifier.weight(1f))

        DialogDismissButton(
            "Cancel",
            onClick = state.onClose,
            enabled = true,
        )
    }
}

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        val dialog = ForgetRepositoryDialog(DocumentsInHDDA.uri)

        dialog.renderDialogCard(
            ForgetRepositoryDialog.State(
                DocumentsInHDDA.storageRepository,
                onLaunch = {},
                onClose = {},
            ),
        )
    }
}
