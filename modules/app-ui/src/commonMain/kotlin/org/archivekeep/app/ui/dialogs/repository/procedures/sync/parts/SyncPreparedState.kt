package org.archivekeep.app.ui.dialogs.repository.procedures.sync.parts

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync
import org.archivekeep.app.ui.components.base.layout.IntrinsicSizeWrapperLayout
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumn
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
                        val state = rememberManySelectWithMergedState(
                            step.operations,
                            userFlowState.selectedOperations
                        )

                        rememberManySelectForRenderFromState(
                            state,
                            "Existing files to replicate:",
                            allItemsLabel = { "All $it replications" },
                            itemLabelText = { it.describe() },
                        )
                    }

                    is DiscoveredNewFilesGroup -> {
                        val state = rememberManySelectWithMergedState(
                            step.operations,
                            userFlowState.selectedOperations
                        )

                        rememberManySelectForRenderFromState(
                            state,
                            "New files to copy:",
                            allItemsLabel = { "All $it new files" },
                            itemLabelText = { it.unmatchedBaseExtra.filenames.let { if (it.size == 1) it[0] else it.toString() } },
                        )
                    }

                    is DiscoveredRelocationsMoveApplyGroup -> {
                        val state = rememberManySelectWithMergedState(
                            step.operations,
                            userFlowState.selectedOperations
                        )

                        rememberManySelectForRenderFromStateAnnotated(
                            state = state,
                            label = "Relocations to execute:",
                            allItemsLabel = { "All relocations ($it)" },
                            itemAnnotatedLabel = { it.describe() },
                        )
                    }
                }
            }

    val guessedWidth = blocks.maxOf { it.guessedWidth }

    IntrinsicSizeWrapperLayout(
        minIntrinsicWidth = guessedWidth,
        maxIntrinsicWidth = guessedWidth,
    ) {
        ScrollableLazyColumn(Modifier.Companion.weight(1f, fill = false)) {
            blocks.forEach { it.render(this) }
        }
    }
}
