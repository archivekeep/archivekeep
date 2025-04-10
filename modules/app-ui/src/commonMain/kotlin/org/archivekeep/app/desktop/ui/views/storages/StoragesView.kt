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
import org.archivekeep.app.desktop.ui.views.storages.components.AllStoragesList
import org.archivekeep.app.desktop.utils.collectLoadableFlow

class StoragesView : View<StoragesVM> {
    @Composable
    override fun producePersistentState(scope: CoroutineScope): StoragesVM {
        val storageService = LocalStorageService.current

        return remember(scope, storageService) {
            StoragesVM(
                scope,
                storageService,
            )
        }
    }

    @Composable
    override fun render(
        modifier: Modifier,
        state: StoragesVM,
    ) {
        Surface(
            modifier = modifier,
            color = CColors.cardsGridBackground,
        ) {
            ViewScrollableContainer {
                SectionBlock("All storages") {
                    AllStoragesList(state.allStorages.collectLoadableFlow())
                }
            }
        }
    }
}
