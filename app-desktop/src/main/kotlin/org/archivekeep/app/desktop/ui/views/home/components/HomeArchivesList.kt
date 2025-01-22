package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import compose.icons.TablerIcons
import compose.icons.tablericons.Folder
import org.archivekeep.app.desktop.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.richcomponents.ArchiveDropdownIconLaunched
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCard
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardButton
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitle
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitleIconButton
import org.archivekeep.app.desktop.ui.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.desktop.ui.views.home.HomeArchiveEntryViewModel
import org.archivekeep.utils.Loadable

val TinyRoundShape = RoundedCornerShape(4.dp)

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
@OptIn(ExperimentalLayoutApi::class)
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
                SectionCardTitleIconButton(icon = TablerIcons.Folder, onClick = {})
                ArchiveDropdownIconLaunched(
                    repositoryURI = localArchive.primaryRepository.reference.uri,
                    isAssociated = localArchive.archive.associationId != null,
                )
            },
        )

        HomeCardStateText(state.indexStatusText)

        val showAddPushAction = state.canAddPush
        val showAddAction = state.canAdd
        val showPushAction = state.canPush

        if (showAddPushAction || showAddAction || showPushAction) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp,
                            start = sectionCardHorizontalPadding,
                            bottom = 8.dp,
                            end = sectionCardHorizontalPadding,
                        ),
            ) {
                if (showAddPushAction) {
                    SectionCardButton(
                        onClick = {
                            archiveOperationLaunchers.openAddAndPushOperation(
                                localArchive.primaryRepository.reference.uri,
                            )
                        },
                        text = "Add and push",
                        running = state.addPushOperationRunning,
                    )
                }

                if (showAddAction) {
                    SectionCardButton(
                        onClick = {
                            archiveOperationLaunchers.openIndexUpdateOperation(
                                localArchive.primaryRepository.reference.uri,
                            )
                        },
                        text = "Add",
                    )
                }

                if (showPushAction) {
                    SectionCardButton(
                        onClick = {
                            archiveOperationLaunchers.pushRepoToAll(
                                localArchive.primaryRepository.reference.uri,
                            )
                        },
                        text = "Push to all",
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        SectionCardBottomList(
            localArchive.secondaryRepositories.collectAsState().value,
            noItemsText = "No repositories associated …",
        ) { (storage, it) ->
            SecondaryArchiveRepositoryRow(
                it,
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
