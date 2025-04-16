package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import compose.icons.TablerIcons
import compose.icons.tablericons.Folder
import org.archivekeep.app.desktop.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.repository.WithRepositoryOpener
import org.archivekeep.app.desktop.ui.components.richcomponents.ArchiveDropdownIconLaunched
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCard
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardActionsRow
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitle
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitleIconButton
import org.archivekeep.app.desktop.ui.designsystem.styles.CIcons
import org.archivekeep.app.desktop.ui.views.home.HomeArchiveEntryViewModel
import org.archivekeep.app.desktop.ui.views.home.actions
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

@Composable
fun HomeArchivesList(allLocalArchivesLoadable: Loadable<List<HomeArchiveEntryViewModel>>) {
    val archiveOperationLaunchers = LocalArchiveOperationLaunchers.current

    LoadableGuard(allLocalArchivesLoadable) { allLocalArchives ->
        VerticalGrid(
            columns = SimpleGridCells.Adaptive(minSize = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (allLocalArchives.isEmpty()) {
                Text("nothing here ...")
            }

            allLocalArchives.forEach { localArchive ->
                HomeArchiveEntry(
                    localArchive,
                    archiveOperationLaunchers,
                )
            }
        }
    }
}

@Composable
private fun HomeArchiveEntry(
    localArchive: HomeArchiveEntryViewModel,
    archiveOperationLaunchers: ArchiveOperationLaunchers,
) {
    SectionCard {
        val state = localArchive.state.collectAsState().value

        SectionCardTitle(
            state.loading,
            localArchive.displayName,
            icons = {
                WithRepositoryOpener(localArchive.primaryRepository.reference.uri) {
                    SectionCardTitleIconButton(
                        icon = TablerIcons.Folder,
                        onClick = openRepository,
                    )
                }
                ArchiveDropdownIconLaunched(
                    repositoryURI = localArchive.primaryRepository.reference.uri,
                    isAssociated = localArchive.archive.associationId != null,
                )
            },
        )

        HomeCardStateText(state.indexStatusText)

        SectionCardActionsRow(
            state.actions(archiveOperationLaunchers, localArchive),
            noActionsText =
                if (state.canPush.mapIfLoadedOrDefault(false) { it }) {
                    "Copies out of sync."
                } else {
                    "No available actions."
                },
        )

        SectionCardBottomList(
            localArchive.secondaryRepositories.collectAsState().value,
            noItemsText = "No repositories associated …",
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
