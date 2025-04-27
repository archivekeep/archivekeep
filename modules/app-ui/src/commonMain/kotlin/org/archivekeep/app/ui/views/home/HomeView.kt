package org.archivekeep.app.ui.views.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewLoading
import org.archivekeep.app.ui.components.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.ui.components.designsystem.sections.SectionBlock
import org.archivekeep.app.ui.components.designsystem.theme.CColors
import org.archivekeep.app.ui.components.feature.WelcomeText
import org.archivekeep.app.ui.domain.wiring.LocalAddPushService
import org.archivekeep.app.ui.domain.wiring.LocalArchiveService
import org.archivekeep.app.ui.domain.wiring.LocalRepoService
import org.archivekeep.app.ui.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.app.ui.views.View
import org.archivekeep.app.ui.views.home.components.HomeArchivesIntro
import org.archivekeep.app.ui.views.home.components.HomeArchivesList
import org.archivekeep.app.ui.views.home.components.HomeNonLocalArchivesList
import org.archivekeep.app.ui.views.home.components.HomeStoragesIntro
import org.archivekeep.app.ui.views.home.components.HomeStoragesList
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapLoadedData

class HomeView : View<HomeViewModel> {
    @Composable
    override fun produceViewModel(scope: CoroutineScope): HomeViewModel {
        val archiveService = LocalArchiveService.current
        val repositoryService = LocalRepoService.current
        val storageService = LocalStorageService.current
        val syncService = LocalRepoToRepoSyncService.current
        val addPushOperationService = LocalAddPushService.current

        return remember(
            scope,
            archiveService,
            storageService,
            syncService,
            addPushOperationService,
        ) {
            HomeViewModel(
                scope,
                archiveService,
                repositoryService,
                storageService,
                syncService,
                addPushOperationService,
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
            val allLocalArchivesLoadable = vm.allLocalArchivesFlow.collectLoadableFlow()
            val otherArchives = vm.otherArchivesFlow.collectLoadableFlow()
            val externalStoragesLoadable = vm.allStoragesFlow.collectLoadableFlow()

            val state = HomeViewState(allLocalArchivesLoadable, otherArchives, externalStoragesLoadable)

            homeViewContent(state)
        }
    }
}

@Composable
private fun homeViewContent(state: HomeViewState) {
    if (state.showBaseLoading) {
        ViewLoading()
        return
    }

    ViewScrollableContainer {
        if (state.showLocalAddIntro) {
            WelcomeText()

            SectionBlock("Introduction") {
                HomeArchivesIntro()
                if (state.showExternalAddIntro) {
                    Spacer(Modifier.height(16.dp))
                    HomeStoragesIntro()
                }
            }
        } else {
            SectionBlock("Local archives") {
                HomeArchivesList(state.allLocalArchivesLoadable)
            }
        }

        state.otherArchives.let { otherArchives ->
            if (otherArchives is Loadable.Loaded && otherArchives.value.isNotEmpty()) {
                SectionBlock("External archives") {
                    HomeNonLocalArchivesList(otherArchives)
                }
            }
        }

        if (state.showExternalAddIntro) {
            if (!state.showLocalAddIntro) {
                SectionBlock("Introduction") {
                    HomeStoragesIntro()
                }
            }
        } else {
            if (state.showExternalStoragesSection) {
                SectionBlock(
                    "External storages",
                    isLoading = state.externalStoragesLoadable.mapIfLoadedOrDefault(true) { it.isLoadingSomeItems },
                ) {
                    HomeStoragesList(state.externalStoragesLoadable.mapLoadedData { it.availableStorages })
                }
            }
        }
    }
}
