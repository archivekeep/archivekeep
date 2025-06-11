package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.wiring.LocalRegistry
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.asTrivialAction
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.utils.simpleLaunchable
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

        val launchable =
            simpleLaunchable(scope) {
                registry.updateRepositories { old ->
                    old.filter { it.uri != this@VM.uri }.toSet()
                }
                onClose()
            }

        override fun onClose() {
            _onClose()
        }
    }

    data class State(
        val currentRepo: StorageRepository,
        val launchable: Launchable<Unit>,
        val onClose: () -> Unit,
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Forget repository")
            }

        val action by launchable.asTrivialAction()
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
                State(storageRepository, vm.launchable, vm::onClose)
            }
        }.collectLoadableFlow()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        Text(
            remember(state.currentRepo) {
                buildAnnotatedString {
                    append("Repository ")
                    appendBoldSpan(state.currentRepo.repositoryState.displayName)
                    append(" stored in ")
                    appendBoldSpan(state.currentRepo.storage.displayName)
                    append(" will be removed from local registry of known repositories.")
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
        LaunchableExecutionErrorIfPresent(state.launchable)
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        SimpleActionDialogControlButtons(
            "Forget",
            actionState = state.action,
            onClose = state.onClose,
        )
    }
}
