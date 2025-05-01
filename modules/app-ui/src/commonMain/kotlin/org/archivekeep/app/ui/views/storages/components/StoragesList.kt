package org.archivekeep.app.ui.views.storages.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import org.archivekeep.app.ui.components.designsystem.elements.ConnectionStatusTag
import org.archivekeep.app.ui.components.designsystem.sections.EmptySectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitle
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardItem
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.StorageDropdownIconLaunched
import org.archivekeep.app.ui.views.storages.StoragesViewState
import org.archivekeep.utils.loading.Loadable

@Composable
fun StoragesList(
    allStoragesLoadable: Loadable<List<StoragesViewState.Storage>>,
    emptyText: String,
) {
    LoadableGuard(allStoragesLoadable) { allStorages ->
        VerticalGrid(
            columns = SimpleGridCells.Adaptive(minSize = 250.dp),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingHorizontal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.gridSpacingVertical),
        ) {
            if (allStorages.isEmpty()) {
                EmptySectionCard(emptyText)
            }

            allStorages.forEach { StorageEntry(it) }
        }
    }
}

@Composable
private fun StorageEntry(storage: StoragesViewState.Storage) {
    SectionCard {
        SectionCardTitle(
            // TODO
            false,
            storage.displayName,
            icons = {
                StorageDropdownIconLaunched(storage.uri)
            },
        )

        Row(Modifier.sectionCardItem().padding(top = 5.dp, bottom = 8.dp)) {
            ConnectionStatusTag(storage.connectionStatus)
        }

        Spacer(Modifier.height(4.dp))

        SectionCardBottomList(storage.repositoriesInThisStorage) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 4.dp,
                            horizontal = sectionCardHorizontalPadding,
                        ),
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                ) {
                    val name = it.displayName

                    Text(
                        name,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}
