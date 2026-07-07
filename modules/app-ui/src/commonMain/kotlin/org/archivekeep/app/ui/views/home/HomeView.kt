package org.archivekeep.app.ui.views.home

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.ui.components.designsystem.theme.CColors
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.views.View
import org.archivekeep.app.ui.views.home.components.HomeViewContent
import org.archivekeep.app.ui.views.home.model.HomeViewState

class HomeView : View<HomeViewModel> {
    @Composable
    override fun produceViewModel(scope: CoroutineScope): HomeViewModel {
        val services = LocalApplicationServices.current

        return remember(scope, services) {
            HomeViewModel(
                scope,
                services.archiveService,
                services.repositoryService,
                services.storageService,
                services.syncService,
                services.addPushService,
                services.addOperationSupervisorService,
            )
        }
    }

    @Composable
    override fun render(
        modifier: Modifier,
        vm: HomeViewModel,
    ) {
        Surface(
            modifier,
            color = CColors.cardsGridBackground,
        ) {
            HomeViewContent(
                HomeViewState(
                    vm.allLocalArchivesFlow.collectLoadableFlow(),
                    vm.otherArchivesFlow.collectLoadableFlow(),
                    vm.allStoragesFlow.collectLoadableFlow(),
                ),
            )
        }
    }
}
