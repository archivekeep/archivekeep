package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.storages.needsUnlock
import org.archivekeep.app.ui.components.designsystem.elements.WarningBadge
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitle
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme
import org.archivekeep.app.ui.components.designsystem.theme.CIcons
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.model.HomeNonLocalArchiveUiState
import org.archivekeep.app.ui.views.home.model.RepositoryOperationalState
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapLoadedData

@Composable
fun HomeNonLocalArchivesList(otherArchivesLoadable: Loadable<List<HomeNonLocalArchiveUiState>>) {
    LoadableGuard(otherArchivesLoadable) { nonLocalArchives ->
        VerticalGrid(
            columns = SimpleGridCells.Adaptive(minSize = 250.dp),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingVertical),
        ) {
            if (nonLocalArchives.isEmpty()) {
                Text("Empty")
            }

            nonLocalArchives.forEach { nonLocalArchive ->
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

                    SectionCardBottomList(nonLocalArchive.otherRepositories) { repo ->
                        val name =
                            repo.storageReference.displayName + (
                                if (repo.reference.displayName != nonLocalArchive.displayName) {
                                    " (${repo.reference.displayName})"
                                } else {
                                    ""
                                }
                            )

                        // TODO: move out logic from UI
                        val repositoryOperationalState =
                            RepositoryOperationalState(
                                repo.repository.optionalAccessorFlow
                                    .collectAsState()
                                    .value,
                                isAssociated = false,
                                repo.repository.localRepoStatus
                                    .collectAsState()
                                    .value
                                    .mapLoadedData { it.summary },
                            )

                        val launchers = LocalArchiveOperationLaunchers.current
                        val ra = repositoryOperationalState.actions(launchers, repo.repository.uri)

                        RepositoryRow(
                            statusText = OptionalLoadable.LoadedAvailable(""), // TODO: implement status text
                            isLoading = false, // TODO: implement loading indication
                            isConnected = true,
                            needsUnlock =
                                remember {
                                    repo.repository
                                        .optionalAccessorFlow
                                        .map { it.needsUnlock() }
                                }.collectAsState(false)
                                    .value,
                            name = name,
                            icon = CIcons.Repository,
                            iconActions = ra.iconActions(),
                            secondaryRepositoryActions = emptyList(),
                            repositoryActions = ra,
                        )
                    }
                }
            }
        }
    }
}
