package org.archivekeep.app.desktop.ui.views.storages.components

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
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.richcomponents.StorageDropdownIconLaunched
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCard
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardActionsRow
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitle
import org.archivekeep.app.desktop.ui.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.desktop.ui.views.storages.StoragesViewState
import org.archivekeep.utils.loading.Loadable

@Composable
fun StoragesList(allStoragesLoadable: Loadable<List<StoragesViewState.Storage>>) {
    LoadableGuard(allStoragesLoadable) { allStorages ->
        VerticalGrid(
            columns = SimpleGridCells.Adaptive(minSize = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (allStorages.isEmpty()) {
                Text("Nothing here")
            }

            allStorages.forEach { storage ->
                SectionCard {
                    SectionCardTitle(
                        // TODO
                        false,
                        storage.displayName,
                        icons = {
                            StorageDropdownIconLaunched(storage.uri)
                        },
                    )

                    SectionCardActionsRow(
                        emptyList(),
                        noActionsText = if (storage.isLocal) "Local storage" else "External storage",
                    )

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
        }
    }
}
