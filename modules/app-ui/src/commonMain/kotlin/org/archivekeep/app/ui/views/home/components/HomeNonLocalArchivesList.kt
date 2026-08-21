package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.elements.WarningBadge
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardGrid
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitle
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.ui.components.designsystem.theme.CIcons
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.components.util.baseActions
import org.archivekeep.app.ui.views.home.model.HomeExternalArchiveModel
import org.archivekeep.utils.loading.Loadable

@Composable
fun HomeNonLocalArchivesList(otherArchivesLoadable: Loadable<List<HomeExternalArchiveModel>>) {
    LoadableGuard(otherArchivesLoadable) { nonLocalArchives ->
        SectionCardGrid {
            if (nonLocalArchives.isEmpty()) {
                Text("Empty")
            }

            nonLocalArchives.forEach { nonLocalArchive ->
                val uiState = nonLocalArchive.state.collectAsState().value

                SectionCard {
                    SectionCardTitle(
                        false,
                        nonLocalArchive.displayName,
                        icons = {
                        },
                    )

                    if (!nonLocalArchive.isAssociated) {
                        Row(Modifier.padding(horizontal = sectionCardHorizontalPadding, vertical = 4.dp)) {
                            WarningBadge { Text("Unassociated") }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    SectionCardBottomList(uiState.repositories) { repo ->
                        val launchers = LocalArchiveOperationLaunchers.current
                        val ra = repo.repositoryOperationalState.actions(launchers, repo.uri)

                        RepositoryRow(
                            statusText = repo.statusText,
                            isLoading = repo.isLoading,
                            connectionStatus = repo.connectionStatus,
                            name = repo.name,
                            icon = CIcons.Repository,
                            iconActions = ra.baseActions(),
                            secondaryRepositoryActions = emptyList(),
                            repositoryActions = ra,
                        )
                    }
                }
            }
        }
    }
}
