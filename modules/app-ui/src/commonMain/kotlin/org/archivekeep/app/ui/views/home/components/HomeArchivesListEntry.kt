package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.elements.WarningBadge
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardActionsRow
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitle
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitleIconButton
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.ui.components.designsystem.theme.CIcons
import org.archivekeep.app.ui.components.feature.ArchiveDropdownIconLaunched
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveModel
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.home_archive_copies_out_of_sync
import org.archivekeep.ui.resources.home_archive_no_actions
import org.archivekeep.ui.resources.home_archive_no_repositories
import org.archivekeep.ui.resources.home_archive_unassociated
import org.archivekeep.ui.resources.home_index_status
import org.archivekeep.ui.resources.home_index_status_with_unindexed
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeArchivesListEntry(
    localArchive: HomeLocalArchiveModel,
    archiveOperationLaunchers: ArchiveOperationLaunchers,
) {
    SectionCard {
        val state = localArchive.state.collectAsState().value

        val actions = state.actions(archiveOperationLaunchers, localArchive)

        SectionCardTitle(
            state.loading,
            localArchive.displayName,
            icons = {
                (actions.open as? Loadable.Loaded) ?.let {
                    if (it.value.isAvailable) {
                        SectionCardTitleIconButton(
                            icon = it.value.icon!!,
                            onClick = it.value.onLaunch,
                        )
                    }
                }
                ArchiveDropdownIconLaunched(actions)
            },
        )

        val indexStatusText: OptionalLoadable<String> = when (val status = state.indexStatus) {
            is OptionalLoadable.LoadedAvailable -> {
                val (indexed, newFiles) = status.value
                val text = if (newFiles > 0) {
                    stringResource(Res.string.home_index_status_with_unindexed, indexed, newFiles)
                } else {
                    stringResource(Res.string.home_index_status, indexed)
                }
                OptionalLoadable.LoadedAvailable(text)
            }
            else -> OptionalLoadable.Loading
        }
        HomeCardStateText(indexStatusText)

        if (!localArchive.isAssociated) {
            Row(Modifier.padding(horizontal = sectionCardHorizontalPadding, vertical = 4.dp)) {
                WarningBadge { Text(stringResource(Res.string.home_archive_unassociated)) }
            }
        }

        SectionCardActionsRow(
            actions.asList(),
            noActionsText =
                if (state.canPush.mapIfLoadedOrDefault(false) { it }) {
                    stringResource(Res.string.home_archive_copies_out_of_sync)
                } else {
                    stringResource(Res.string.home_archive_no_actions)
                },
        )

        SectionCardBottomList(
            localArchive.secondaryRepositories.collectAsState().value,
            noItemsText = stringResource(Res.string.home_archive_no_repositories),
        ) { (storage, it) ->
            SecondaryArchiveRepositoryRow(
                it,
                icon = CIcons.Storage,
                name =
                    storage.namedReference.displayName + (
                        if (it.repo.reference.displayName != localArchive.displayName) {
                            " (${it.repo.reference.displayName})"
                        } else {
                            ""
                        }
                    ),
            )
        }
    }
}
