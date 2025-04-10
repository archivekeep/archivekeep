package org.archivekeep.app.desktop.ui.views.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.desktop.domain.wiring.LocalAddPushService
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveService
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.components.various.WelcomeText
import org.archivekeep.app.desktop.ui.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionBlock
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors
import org.archivekeep.app.desktop.ui.views.View
import org.archivekeep.app.desktop.ui.views.home.components.HomeActionsList
import org.archivekeep.app.desktop.ui.views.home.components.HomeArchivesIntro
import org.archivekeep.app.desktop.ui.views.home.components.HomeArchivesList
import org.archivekeep.app.desktop.ui.views.home.components.HomeNonLocalArchivesList
import org.archivekeep.app.desktop.ui.views.home.components.HomeStoragesIntro
import org.archivekeep.app.desktop.ui.views.home.components.HomeStoragesList
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.isLoading

class HomeView : View<HomeViewModel> {
    @Composable
    override fun producePersistentState(scope: CoroutineScope): HomeViewModel {
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
        state: HomeViewModel,
    ) {
        Surface(
            modifier,
            color = CColors.cardsGridBackground,
        ) {
            homeViewContent(state)
        }
    }
}

@Composable
private fun homeViewContent(vm: HomeViewModel) {
    val allLocalArchivesLoadable = vm.allLocalArchives.collectLoadableFlow()
    val externalStoragesLoadable = vm.allStorages.collectLoadableFlow()

    if (allLocalArchivesLoadable.isLoading || externalStoragesLoadable.isLoading) {
        Text("Loading ...")
        return
    }

    val showLocalAddIntro = if (allLocalArchivesLoadable is Loadable.Loaded) allLocalArchivesLoadable.value.isEmpty() else false
    val showExternalAddIntro = if (externalStoragesLoadable is Loadable.Loaded) externalStoragesLoadable.value.isEmpty() else false

    val showLocalAddButton = !showLocalAddIntro
    val showExternalAddButton = !showExternalAddIntro

    ViewScrollableContainer {
        if (showLocalAddIntro) {
            WelcomeText()

            SectionBlock("Introduction") {
                HomeArchivesIntro()
                if (showExternalAddIntro) {
                    Spacer(Modifier.height(16.dp))
                    HomeStoragesIntro()
                }
            }
        } else {
            SectionBlock("Local archives") {
                HomeArchivesList(allLocalArchivesLoadable)
            }
        }

        vm.otherArchivesFlow.collectLoadableFlow().let { otherArchives ->
            if (otherArchives is Loadable.Loaded && otherArchives.value.isNotEmpty()) {
                SectionBlock("External archives") {
                    HomeNonLocalArchivesList(otherArchives)
                }
            }
        }

        if (showExternalAddIntro) {
            if (!showLocalAddIntro) {
                SectionBlock("Introduction") {
                    HomeStoragesIntro()
                }
            }
        } else {
            SectionBlock("External storages") {
                HomeStoragesList(externalStoragesLoadable)
            }
        }

        if (showLocalAddButton || showExternalAddButton) {
            SectionBlock("More") {
                HomeActionsList(
                    allActions =
                        listOfNotNull(
                            if (showLocalAddButton) {
                                HomeViewAction(
                                    "Add local file repository …",
                                    onTrigger =
                                        LocalArchiveOperationLaunchers.current.let {
                                            {
                                                it.openAddFileSystemRepository(FileSystemStorageType.LOCAL)
                                            }
                                        },
                                )
                            } else {
                                null
                            },
                            if (showExternalAddButton) {
                                HomeViewAction(
                                    "Add external file repository …",
                                    onTrigger =
                                        LocalArchiveOperationLaunchers.current.let {
                                            {
                                                it.openAddFileSystemRepository(FileSystemStorageType.EXTERNAL)
                                            }
                                        },
                                )
                            } else {
                                null
                            },
                            if (showExternalAddButton) {
                                HomeViewAction(
                                    "Add remote repository …",
                                    onTrigger =
                                        LocalArchiveOperationLaunchers.current.let {
                                            {
                                                it.openAddRemoteRepository()
                                            }
                                        },
                                )
                            } else {
                                null
                            },
                        ),
                )
            }
        }
    }
}
