package org.archivekeep.app.desktop.ui.dialogs.repository.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.archives.AssociatedArchive
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.operations.AssociateRepositoryOperation
import org.archivekeep.app.core.operations.AssociateRepositoryOperation.Target
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveService
import org.archivekeep.app.desktop.domain.wiring.LocalOperationFactory
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.domain.wiring.OperationFactory
import org.archivekeep.app.desktop.ui.components.errors.AutomaticErrorMessage
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.desktop.ui.dialogs.repository.management.AssociateRepositoryDialog.VM
import org.archivekeep.app.desktop.utils.produceState
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData

class AssociateRepositoryDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<VM.State, VM>(uri) {
    class VM(
        val scope: CoroutineScope,
        val archiveService: ArchiveService,
        val repositoryService: RepositoryService,
        val repository: Repository,
        val operationFactory: OperationFactory,
        val _onClose: () -> Unit,
    ) : IVM<VM.State> {
        data class State(
            val syncCandidates: List<AssociatedArchive>,
            val currentArchive: AssociatedArchive,
            val currentRepo: Pair<Storage, ResolvedRepositoryState>,
        ) : BaseState {
            override val title =
                buildAnnotatedString {
                    if (currentArchive.associationId != null) {
                        append("Re-associate repository")
                    } else {
                        append("Associate repository")
                    }
                }
        }

        val uri = repository.uri

        var selectedItem by mutableStateOf<Target?>(null)
        var lastLaunch by mutableStateOf<Deferred<ExecutionOutcome>?>(null)

        val operation =
            operationFactory.get(AssociateRepositoryOperation.Factory::class.java).create(
                scope,
                uri,
            )

        override val stateFlow =
            archiveService
                .allArchives
                .mapLoadedData { allArchives ->
                    val syncCandidates =
                        allArchives
                            .filter {
                                it.repositories.none { it.second.uri == uri }
                            }

                    val currentArchive =
                        allArchives
                            .first { it.repositories.any { it.second.uri == uri } }

                    val currentRepo = currentArchive.repositories.first { it.second.uri == uri }

                    State(syncCandidates, currentArchive, currentRepo)
                }.stateIn(scope, SharingStarted.WhileSubscribed(), Loadable.Loading)

        override fun onClose() {
            _onClose()
        }

        fun launch() {
            lastLaunch =
                scope.async {
                    val result = operation.execute(selectedItem ?: throw IllegalStateException("Must select first"))

                    when (result) {
                        is ExecutionOutcome.Failed -> {
                            println("Error: ${result.cause}")
                            result.cause.printStackTrace()
                        }
                        is ExecutionOutcome.Success -> onClose()
                    }

                    result
                }
        }
    }

    @Composable
    override fun createVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): VM {
        val archiveService = LocalArchiveService.current
        val repositoryService = LocalRepoService.current
        val operationLaunchers = LocalOperationFactory.current

        return remember(scope, repository, operationLaunchers) {
            VM(
                scope,
                archiveService,
                repositoryService,
                repository,
                operationLaunchers,
                onClose,
            )
        }
    }

    @Composable
    override fun renderContent(
        vm: VM,
        state: VM.State,
    ) {
        val (syncCandidates, currentArchive, currentRepo) = state

        Text(
            remember(currentArchive, currentRepo) {
                buildAnnotatedString {
                    if (currentArchive.associationId != null) {
                        append("Re-associate repository ")
                    } else {
                        append("Associate repository ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(currentRepo.second.displayName)
                    }
                    append(" in storage ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(currentRepo.first.label)
                    }
                    append(" to:")
                }
            },
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            syncCandidates
                .forEach { aa ->
                    val associationId = aa.associationId ?: return@forEach

                    val selected =
                        vm.selectedItem.let { it is Target.Archive && it.associatedArchiveId == aa.associationId }

                    Surface(
                        onClick = {
                            vm.selectedItem = Target.Archive(associationId)
                        },
                        selected = selected,
                        shape = MaterialTheme.shapes.small,
                        color = Color.Transparent,
                        border =
                            if (selected) {
                                BorderStroke(
                                    1.dp,
                                    Color.Gray,
                                )
                            } else {
                                null
                            },
                        modifier = Modifier.padding(bottom = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                            )
                            Column {
                                Text(
                                    text = aa.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                                aa.repositories.forEach {
                                    Text(
                                        " - ${it.first.label} / ${it.second.displayName}",
                                        fontSize = 10.sp,
                                        lineHeight = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                }

            val unAssociatedRepositories =
                syncCandidates
                    .filter { it.associationId == null }

            if (unAssociatedRepositories.isNotEmpty()) {
                Text("Or, create a new archive by association with unassociated repository: ")
            }

            unAssociatedRepositories
                .forEach { aa ->
                    aa.repositories.forEach { (_, repo) ->
                        val selected =
                            vm.selectedItem.let { it is Target.UnassociatedRepository && it.repositoryURI == repo.uri }

                        Surface(
                            onClick = {
                                vm.selectedItem = Target.UnassociatedRepository(repo.uri)
                            },
                            selected = selected,
                            shape = MaterialTheme.shapes.small,
                            color = Color.Transparent,
                            border =
                                if (selected) {
                                    BorderStroke(
                                        1.dp,
                                        Color.Gray,
                                    )
                                } else {
                                    null
                                },
                            modifier = Modifier.padding(bottom = 12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null,
                                )
                                Column {
                                    Text(
                                        text = aa.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

            vm.lastLaunch?.let { lastLaunch ->
                lastLaunch.produceState().value.let { result ->
                    if (result is ExecutionOutcome.Failed) {
                        AutomaticErrorMessage(result, onResolve = { vm.lastLaunch = null })
                    }
                }
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(vm: VM) {
        DialogButtonContainer {
            DialogPrimaryButton(
                "Associate",
                onClick = vm::launch,
                enabled = vm.selectedItem != null,
            )

            Spacer(modifier = Modifier.weight(1f))

            DialogDismissButton(
                "Cancel",
                onClick = vm::onClose,
                enabled = true,
            )
        }
    }
}
