package org.archivekeep.app.desktop.ui.views.storages

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionBlock
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors
import org.archivekeep.app.desktop.ui.views.View
import org.archivekeep.app.desktop.ui.views.storages.components.StoragesList
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.mapLoadedData

class StoragesView : View<StoragesViewModel> {
    @Composable
    override fun produceViewModel(scope: CoroutineScope): StoragesViewModel {
        val storageService = LocalStorageService.current

        return remember(scope, storageService) {
            StoragesViewModel(
                scope,
                storageService,
            )
        }
    }

    @Composable
    override fun render(
        modifier: Modifier,
        vm: StoragesViewModel,
    ) {
        val state = vm.state.collectLoadableFlow()

        Surface(
            modifier = modifier,
            color = CColors.cardsGridBackground,
        ) {
            ViewScrollableContainer {
                SectionBlock("Local storages") {
                    StoragesList(
                        state.mapLoadedData { it.localStorages },
                        emptyText = "No local storages registered. Add repository belonging to a local storage.",
                    )
                }
                SectionBlock("External storages") {
                    StoragesList(
                        state.mapLoadedData { it.externalStorages },
                        emptyText = "No external storages registered. Add repository belonging to an external storage.",
                    )
                }
                SectionBlock("Online storages") {
                    StoragesList(
                        state.mapLoadedData { it.onlineStorages },
                        emptyText = "No online storages registered. Add repository from online storage.",
                    )
                }
            }
        }
    }
}
