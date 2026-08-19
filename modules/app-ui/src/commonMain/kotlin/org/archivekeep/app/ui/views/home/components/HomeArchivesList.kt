package org.archivekeep.app.ui.views.home.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardGrid
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveModel
import org.archivekeep.utils.loading.Loadable

@Composable
fun HomeArchivesList(localArchivesListLoadable: Loadable<List<HomeLocalArchiveModel>>) {
    val archiveOperationLaunchers = LocalArchiveOperationLaunchers.current

    LoadableGuard(localArchivesListLoadable) { allLocalArchives ->
        SectionCardGrid {
            if (allLocalArchives.isEmpty()) {
                Text("nothing here ...")
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
