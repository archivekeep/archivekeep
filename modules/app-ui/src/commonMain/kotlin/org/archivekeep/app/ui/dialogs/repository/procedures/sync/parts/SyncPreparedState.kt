package org.archivekeep.app.ui.dialogs.repository.procedures.sync.parts

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumnWithGuessedWidth
import org.archivekeep.app.ui.components.feature.manyselect.ManySelectForRender
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRenderFromState
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRenderFromStateAnnotated
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectWithMergedState
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.RepoToRepoSyncUserFlow
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.describe
import org.archivekeep.files.procedures.sync.discovery.DiscoveredAdditiveRelocationsGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredNewFilesGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredRelocationsMoveApplyGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredSyncOperationsGroup
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.manyselect_existing_files_replicate
import org.archivekeep.ui.resources.manyselect_existing_files_replicate_all
import org.archivekeep.ui.resources.manyselect_new_files_copy
import org.archivekeep.ui.resources.manyselect_new_files_copy_all
import org.archivekeep.ui.resources.manyselect_relocations
import org.archivekeep.ui.resources.manyselect_relocations_all
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ColumnScope.SyncPreparedState(
    operation: RepoToRepoSync.State.Prepared,
    userFlowState: RepoToRepoSyncUserFlow.State,
) {
    val blocks =
        operation.discoveredSync.groups
            .map<DiscoveredSyncOperationsGroup<*>, ManySelectForRender<*, *, *>> { step ->
                when (step) {
                    is DiscoveredAdditiveRelocationsGroup -> {
                        val state =
                            rememberManySelectWithMergedState(
                                step.operations,
                                userFlowState.selectedOperations,
                            )

                        rememberManySelectForRenderFromState(
                            state,
                            stringResource(Res.string.manyselect_existing_files_replicate),
                            allItemsLabel = { stringResource(Res.string.manyselect_existing_files_replicate_all, it.toString()) },
                            itemLabelText = { it.describe() },
                        )
                    }

                    is DiscoveredNewFilesGroup -> {
                        val state =
                            rememberManySelectWithMergedState(
                                step.operations,
                                userFlowState.selectedOperations,
                            )

                        rememberManySelectForRenderFromState(
                            state,
                            stringResource(Res.string.manyselect_new_files_copy),
                            allItemsLabel = { stringResource(Res.string.manyselect_new_files_copy_all, it.toString()) },
                            itemLabelText = { it.unmatchedBaseExtra.filenames.let { if (it.size == 1) it[0] else it.toString() } },
                        )
                    }

                    is DiscoveredRelocationsMoveApplyGroup -> {
                        val state =
                            rememberManySelectWithMergedState(
                                step.operations,
                                userFlowState.selectedOperations,
                            )

                        rememberManySelectForRenderFromStateAnnotated(
                            state = state,
                            label = stringResource(Res.string.manyselect_relocations),
                            allItemsLabel = { stringResource(Res.string.manyselect_relocations_all, it.toString()) },
                            itemAnnotatedLabel = { it.describe() },
                        )
                    }
                }
            }

    val guessedWidth = blocks.maxOf { it.guessedWidth }

    ScrollableLazyColumnWithGuessedWidth(guessedWidth, Modifier.weight(1f, fill = false)) {
        blocks.forEach { it.render(this) }
    }
}
