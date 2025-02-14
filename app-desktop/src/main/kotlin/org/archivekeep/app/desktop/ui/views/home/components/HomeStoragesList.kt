package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cheonjaeung.compose.grid.SimpleGridCells
import com.cheonjaeung.compose.grid.VerticalGrid
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.richcomponents.StorageDropdownIconLaunched
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCard
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardButton
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitle
import org.archivekeep.app.desktop.ui.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.app.desktop.ui.designsystem.styles.CIcons
import org.archivekeep.app.desktop.ui.views.home.HomeViewStorage
import org.archivekeep.utils.Loadable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeStoragesList(allStoragesFlow: Loadable<List<HomeViewStorage>>) {
    LoadableGuard(allStoragesFlow) { allStorages ->
        VerticalGrid(
            columns = SimpleGridCells.Adaptive(minSize = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (allStorages.isEmpty()) {
                Text("Nothing here ...")
            }

            allStorages.forEach { storage ->
                val storageState by storage.stateFlow.collectAsState()

                SectionCard(
                    enabled = storageState.isConnected,
                ) {
                    SectionCardTitle(
                        // TODO
                        false,
                        storage.name ?: "Unnamed filesystem",
                        grayOutText = storage.name == null || !storageState.isConnected,
                        subtitle =
                            if (storage.name == null) {
                                {
                                    Text(
                                        storage.reference.displayName,
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                    )
                                }
                            } else {
                                null
                            },
                        icons = {
                            StorageDropdownIconLaunched(storage.reference.uri)
                        },
                    )

                    val canPushAny = storageState.canPushAny
                    val canPullAny = storageState.canPullAny

                    if (canPushAny || canPullAny) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            if (canPushAny) {
                                SectionCardButton(
                                    onClick =
                                        LocalArchiveOperationLaunchers.current.let {
                                            {
                                                it.pushAllToStorage(storage.reference.uri)
                                            }
                                        },
                                    text = "Push",
                                )
                            }

                            if (canPullAny) {
                                SectionCardButton(
                                    onClick =
                                        LocalArchiveOperationLaunchers.current.let {
                                            {
                                                it.pullAllFromStorage(storage.reference.uri)
                                            }
                                        },
                                    text = "Pull",
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    SectionCardBottomList(storage.secondaryRepositories.collectAsState().value) { secondaryRepositoryState ->
                        SecondaryArchiveRepositoryRow(secondaryRepositoryState, icon = CIcons.Repository)
                    }
                }
            }
        }
    }
}
