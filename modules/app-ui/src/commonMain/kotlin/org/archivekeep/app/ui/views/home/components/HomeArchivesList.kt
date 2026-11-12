package org.archivekeep.app.ui.views.home.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardGrid
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveModel
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.home_archives_list_empty
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeArchivesList(localArchivesListLoadable: Loadable<List<HomeLocalArchiveModel>>) {
    val archiveOperationLaunchers = LocalArchiveOperationLaunchers.current

    LoadableGuard(localArchivesListLoadable) { allLocalArchives ->
        SectionCardGrid {
            if (allLocalArchives.isEmpty()) {
                Text(stringResource(Res.string.home_archives_list_empty))
            }

            allLocalArchives.forEach { localArchive ->
                HomeArchivesListEntry(
                    localArchive,
                    archiveOperationLaunchers,
                )
            }
        }
    }
}
