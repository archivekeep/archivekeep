package org.archivekeep.app.ui.views.storages

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.ui.components.designsystem.sections.SectionBlock
import org.archivekeep.app.ui.components.designsystem.theme.CColors
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.views.View
import org.archivekeep.app.ui.views.storages.components.StoragesList
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.storages_external
import org.archivekeep.ui.resources.storages_local
import org.archivekeep.ui.resources.storages_no_external
import org.archivekeep.ui.resources.storages_no_local
import org.archivekeep.ui.resources.storages_no_online
import org.archivekeep.ui.resources.storages_online
import org.archivekeep.utils.loading.mapLoadedData
import org.jetbrains.compose.resources.stringResource

class StoragesView : View<StoragesViewModel> {
    @Composable
    override fun produceViewModel(scope: CoroutineScope): StoragesViewModel {
        val storageService = LocalApplicationServices.current.storageService

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
                SectionBlock(stringResource(Res.string.storages_local)) {
                    StoragesList(
                        state.mapLoadedData { it.localStorages },
                        emptyText = stringResource(Res.string.storages_no_local),
                    )
                }
                SectionBlock(stringResource(Res.string.storages_external)) {
                    StoragesList(
                        state.mapLoadedData { it.externalStorages },
                        emptyText = stringResource(Res.string.storages_no_external),
                    )
                }
                SectionBlock(stringResource(Res.string.storages_online)) {
                    StoragesList(
                        state.mapLoadedData { it.onlineStorages },
                        emptyText = stringResource(Res.string.storages_no_online),
                    )
                }
            }
        }
    }
}
