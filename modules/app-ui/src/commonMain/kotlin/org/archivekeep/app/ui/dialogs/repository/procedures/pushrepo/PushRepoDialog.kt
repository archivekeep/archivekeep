package org.archivekeep.app.ui.dialogs.repository.procedures.pushrepo

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.SplitRow
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.ui.components.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardItemStateText
import org.archivekeep.app.ui.components.feature.RelocationSyncModeOptions
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.asMutableState
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.files.procedures.sync.discovery.RelocationSyncMode
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable

class PushRepoDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<PushRepoDialog.State, PushRepoDialogViewModel>(uri) {
    data class State(
        val repoName: String,
        val relocationSyncMode: MutableState<RelocationSyncMode>,
        val otherRepositoryCandidates: androidx.compose.runtime.State<List<PushRepoDialogViewModel.RepoStatus>>,
        val startAllSync: () -> Unit,
        val onClose: () -> Unit,
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                appendBoldSpan(repoName)
                append(" - push")
            }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): PushRepoDialogViewModel {
        val storageService = LocalStorageService.current
        val syncService = LocalRepoToRepoSyncService.current

        return remember {
            PushRepoDialogViewModel(
                scope,
                uri,
                storageService,
                syncService,
                onClose,
            )
        }
    }

    @Composable
    override fun rememberState(vm: PushRepoDialogViewModel): Loadable<State> {
        val relocationSyncMode = vm.relocationSyncModeFlow.asMutableState()
        val otherRepositoryCandidates = vm.otherRepos.collectAsState()

        return remember(vm) {
            vm.repoName.mapToLoadable { repoName ->
                State(
                    repoName,
                    relocationSyncMode,
                    otherRepositoryCandidates,
                    startAllSync = vm::startAllSync,
                    onClose = vm::onClose,
                )
            }
        }.collectLoadableFlow()
    }

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        RelocationSyncModeOptions(
            state.relocationSyncMode.value,
            onRelocationSyncModeChange = {
                state.relocationSyncMode.value = it
            },
        )

        Spacer(Modifier.height(4.dp))

        LabelText("Repositories to sync:")

        state.otherRepositoryCandidates.value.forEach {
            SplitRow(
                leftContent = {
                    Text(it.otherRepository.storage.displayName)
                    SectionCardItemStateText(it.statusText.collectLoadableFlow())
                },
                rightContent = {
                    Text("Start")
                },
            )
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogPrimaryButton(
            "Start all",
            onClick = state.startAllSync,
            enabled = true,
        )
        Spacer(modifier = Modifier.weight(1f))

        DialogDismissButton(
            "Dismiss",
            onClick = state.onClose,
            enabled = true,
        )
    }
}
