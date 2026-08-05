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
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

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

        HomeCardStateText(state.indexStatusText)

        if (!localArchive.isAssociated) {
            Row(Modifier.padding(horizontal = sectionCardHorizontalPadding, vertical = 4.dp)) {
                WarningBadge { Text("Unassociated") }
            }
        }

        SectionCardActionsRow(
            actions.asList(),
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
