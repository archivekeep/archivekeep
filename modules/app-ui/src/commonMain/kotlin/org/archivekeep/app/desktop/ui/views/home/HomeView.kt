package org.archivekeep.app.desktop.ui.views.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import org.archivekeep.app.desktop.ui.designsystem.layout.views.ViewScrollableContainer
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionTitle
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
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Welcome to ArchiveKeep",
                modifier = Modifier.padding(top = 20.dp, bottom = 0.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                "Personal archivation - file synchronization and replication across multiple storages (repositories)",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
        }

        Column(
            modifier = Modifier.padding(top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showLocalAddIntro) {
                SectionTitle("Introduction")
                HomeArchivesIntro()
            } else {
                SectionTitle("Local archives")
                HomeArchivesList(allLocalArchivesLoadable)
            }

            vm.otherArchivesFlow.collectLoadableFlow().let { otherArchives ->
                if (otherArchives is Loadable.Loaded && otherArchives.value.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SectionTitle("External archives")
                    HomeNonLocalArchivesList(otherArchives)
                }
            }

            if (showExternalAddIntro) {
                if (!showLocalAddIntro) {
                    SectionTitle("Introduction")
                }
                HomeStoragesIntro()
            } else {
                SectionTitle("External storages")
                HomeStoragesList(externalStoragesLoadable)
            }

            if (showLocalAddButton || showExternalAddButton) {
                Spacer(Modifier.height(12.dp))
                SectionTitle("More")
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
