package org.archivekeep.app.ui.views.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import compose.icons.TablerIcons
import compose.icons.tablericons.Folder
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardActionsRow
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitle
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitleIconButton
import org.archivekeep.app.ui.components.designsystem.theme.CIcons
import org.archivekeep.app.ui.components.feature.ArchiveDropdownIconLaunched
import org.archivekeep.app.ui.components.feature.repository.WithRepositoryOpener
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveModel
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

@Composable
fun HomeArchivesListEntry(
    localArchive: HomeLocalArchiveModel,
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
                    repositoryAccessor =
                        localArchive.repository.optionalAccessorFlow
                            .collectAsState()
                            .value,
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
