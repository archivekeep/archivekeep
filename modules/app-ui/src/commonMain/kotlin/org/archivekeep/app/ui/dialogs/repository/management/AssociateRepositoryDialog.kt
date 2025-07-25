package org.archivekeep.app.ui.dialogs.repository.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.archives.AssociatedArchive
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.operations.AssociateRepositoryOperation
import org.archivekeep.app.core.operations.AssociateRepositoryOperation.Target
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.feature.dialogs.SimpleActionDialogControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.LaunchableExecutionErrorIfPresent
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.management.AssociateRepositoryDialog.VM
import org.archivekeep.app.ui.dialogs.repository.management.AssociateRepositoryDialog.VM.State
import org.archivekeep.app.ui.domain.wiring.LocalArchiveService
import org.archivekeep.app.ui.domain.wiring.LocalOperationFactory
import org.archivekeep.app.ui.domain.wiring.LocalRepoService
import org.archivekeep.app.ui.domain.wiring.OperationFactory
import org.archivekeep.app.ui.utils.Launchable
import org.archivekeep.app.ui.utils.asAction
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.utils.simpleLaunchable
import org.archivekeep.files.RepositoryAssociationGroupId
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData

class AssociateRepositoryDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<State, VM>(uri) {
    class VM(
        val scope: CoroutineScope,
        val archiveService: ArchiveService,
        val repositoryService: RepositoryService,
        val repository: Repository,
        val operationFactory: OperationFactory,
        val _onClose: () -> Unit,
    ) : IVM {
        data class State(
            val syncCandidates: List<AssociatedArchive>,
            val currentAssociationId: RepositoryAssociationGroupId?,
            val currentRepoStorage: KnownStorage,
            val currentRepoInformation: RepositoryInformation,
            val selectedItem: MutableState<Target?>,
            val launchable: Launchable<Unit>,
            val onClose: () -> Unit,
        ) : IState {
            override val title =
                buildAnnotatedString {
                    if (currentAssociationId != null) {
                        append("Re-associate repository")
                    } else {
                        append("Associate repository")
                    }
                }

            val action by launchable.asAction(
                onLaunch = { onLaunch(Unit) },
                canLaunch = { selectedItem.value != null },
            )
        }

        val uri = repository.uri

        val selectedItem = mutableStateOf<Target?>(null)

        val operation =
            operationFactory.get(AssociateRepositoryOperation.Factory::class.java).create(
                scope,
                uri,
            )

        override fun onClose() {
            _onClose()
        }

        val launchable =
            simpleLaunchable(scope) {
                val result =
                    operation.execute(
                        selectedItem.value ?: throw IllegalStateException("Must select first"),
                    )

                when (result) {
                    is ExecutionOutcome.Failed -> {
                        println("Error: ${result.cause}")
                        result.cause.printStackTrace()
                        throw result.cause
                    }

                    is ExecutionOutcome.Success -> onClose()
                }
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
    override fun rememberState(vm: VM): Loadable<State> =
        remember(vm) {
            vm.archiveService
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

                    State(
                        syncCandidates,
                        currentArchive.associationId,
                        currentRepo.first.knownStorage,
                        currentRepo.second.information,
                        vm.selectedItem,
                        launchable = vm.launchable,
                        onClose = vm::onClose,
                    )
                }
        }.collectLoadableFlow()

    @Composable
    override fun ColumnScope.renderContent(state: VM.State) {
        val (syncCandidates, currentAssociationId, currentRepoStorage, currentRepoInformation) = state

        Text(
            remember(currentAssociationId, currentRepoStorage, currentRepoInformation) {
                buildAnnotatedString {
                    if (currentAssociationId != null) {
                        append("Re-associate repository ")
                    } else {
                        append("Associate repository ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(currentRepoInformation.displayName)
                    }
                    append(" in storage ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(currentRepoStorage.label)
                    }
                    append(" to:")
                }
            },
        )

        ScrollableColumn {
            syncCandidates
                .forEach { aa ->
                    val associationId = aa.associationId ?: return@forEach

                    val selected =
                        state.selectedItem.value.let { it is Target.Archive && it.associatedArchiveId == aa.associationId }

                    Surface(
                        onClick = {
                            state.selectedItem.value = Target.Archive(associationId)
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
                            state.selectedItem.value.let { it is Target.UnassociatedRepository && it.repositoryURI == repo.uri }

                        Surface(
                            onClick = {
                                state.selectedItem.value = Target.UnassociatedRepository(repo.uri)
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
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

            LaunchableExecutionErrorIfPresent(state.launchable)
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: VM.State) {
        SimpleActionDialogControlButtons(
            "Associate",
            actionState = state.action,
            onClose = state.onClose,
        )
    }
}
