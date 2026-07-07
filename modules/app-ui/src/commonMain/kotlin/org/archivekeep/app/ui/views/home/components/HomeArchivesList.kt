package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveModel
import org.archivekeep.utils.loading.Loadable

@Composable
fun HomeArchivesList(localArchivesListLoadable: Loadable<List<HomeLocalArchiveModel>>) {
    val archiveOperationLaunchers = LocalArchiveOperationLaunchers.current

    LoadableGuard(localArchivesListLoadable) { allLocalArchives ->
        VerticalGrid(
            columns = SimpleGridCells.Adaptive(minSize = 250.dp),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingVertical),
        ) {
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
