package org.archivekeep.app.desktop.ui.views.storages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionTitle
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
            Column(
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                SectionTitle("All storages")
                AllStoragesList(state.allStorages.collectLoadableFlow())

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
