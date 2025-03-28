package org.archivekeep.app.desktop.ui.dialogs.repository.management

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInLaptopSSD
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveService
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.ui.components.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData

class UnassociateRepositoryDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<UnassociateRepositoryDialog.State, UnassociateRepositoryDialog.VM>(uri) {
    data class State(
        val currentRepoStorage: KnownStorage,
        val currentRepoInformation: RepositoryInformation,
        val onLaunch: () -> Unit,
        val onClose: () -> Unit,
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Unassociate repository")
            }
    }

    inner class VM(
        val coroutineScope: CoroutineScope,
        val archiveService: ArchiveService,
        repositoryService: RepositoryService,
        val _onClose: () -> Unit,
    ) : IVM {
        val repository = repositoryService.getRepository(uri)

        var runningJob by mutableStateOf<Job?>(null)

        fun launch() {
            runningJob =
                coroutineScope.launch {
                    repository.updateMetadata {
                        it.copy(
                            associationGroupId = null,
                        )
                    }
                    onClose()
                }
        }

        override fun onClose() {
            _onClose()
        }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): VM {
        val archiveService = LocalArchiveService.current
        val repositoryService = LocalRepoService.current

        return remember {
            VM(
                scope,
                archiveService,
                repositoryService,
                _onClose = onClose,
            )
        }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember(vm) {
            vm.archiveService
                .allArchives
                .mapLoadedData { allArchives ->
                    val currentArchive =
                        allArchives
                            .first { it.repositories.any { it.second.uri == uri } }

                    val currentRepo = currentArchive.repositories.first { it.second.uri == uri }

                    // TODO: check if already not-associated -> not possible to unassociate

                    State(
                        currentRepo.first.knownStorage,
                        currentRepo.second.information,
                        onLaunch = vm::launch,
                        onClose = vm::onClose,
                    )
                }
        }.collectLoadableFlow()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        Text(
            remember(state.currentRepoStorage, state.currentRepoInformation) {
                buildAnnotatedString {
                    append("Repository ")
                    appendBoldSpan(state.currentRepoInformation.displayName)
                    append(" stored in ")
                    appendBoldSpan(state.currentRepoStorage.label)
                    append(" will be unassociated from its archive.")
                }
            },
        )

        // TODO: add associated repositories with the archive - list them for information
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        SimpleActionDialogControlButtons(
            "Unassociate",
            onLaunch = state.onLaunch,
            onClose = state.onClose,
        )
    }
}

@Composable
@Preview
private fun Preview() {
    DialogPreviewColumn {
        val dialog = UnassociateRepositoryDialog(DocumentsInLaptopSSD.uri)

        dialog.renderDialogCard(
            UnassociateRepositoryDialog.State(
                KnownStorage(DocumentsInLaptopSSD.storage.uri, null, emptyList()),
                RepositoryInformation(null, "A Repo"),
                onLaunch = {},
                onClose = {},
            ),
        )
    }
}
