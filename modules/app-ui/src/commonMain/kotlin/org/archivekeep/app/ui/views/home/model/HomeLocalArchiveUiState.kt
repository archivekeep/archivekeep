package org.archivekeep.app.ui.views.home.model

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.enableUnfinishedFeatures
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable

data class HomeLocalArchiveUiState(
    val canUnlock: Loadable<Boolean>,
    val canAdd: Loadable<Boolean>,
    val canReindex: Loadable<Boolean>,
    val canCleanupDeletedFiles: Loadable<Boolean>,
    val canPush: Loadable<Boolean>,
    val anySecondaryAvailable: Boolean,
    val loading: Boolean,
    val indexStatusText: OptionalLoadable<String>,
    val addPushOperationRunning: Boolean,
    val addOperationRunning: Boolean,
) {
    val canAddPush = if (addPushOperationRunning) Loadable.Loaded(true) else (canAdd.mapLoadedData { it && anySecondaryAvailable })

    @Composable
    fun actions(
        archiveOperationLaunchers: ArchiveOperationLaunchers,
        localArchive: HomeLocalArchiveModel,
    ): List<Loadable<Action>> =
        listOf(
            this.canUnlock.mapLoadedData {
                Action(
                    onLaunch = {
                        archiveOperationLaunchers.unlockRepository(localArchive.primaryRepository.reference.uri, null)
                    },
                    text = "Unlock",
                    isAvailable = it,
                    running = false,
                )
            },
            Loadable.Loaded(
                Action(
                    text = "Associate",
                    isAvailable = !localArchive.isAssociated,
                    onLaunch = {
                        archiveOperationLaunchers.openAssociateRepository(localArchive.primaryRepository.reference.uri)
                    },
                ),
            ),
            this.canAddPush.mapLoadedData {
                Action(
                    onLaunch = {
                        archiveOperationLaunchers.openAddAndPushOperation(
                            localArchive.primaryRepository.reference.uri,
                        )
                    },
                    text = "Add and push",
                    isAvailable = it,
                    running = this.addPushOperationRunning,
                )
            },
            this.canAdd.mapLoadedData {
                Action(
                    onLaunch = {
                        archiveOperationLaunchers.openIndexUpdateOperation(
                            localArchive.primaryRepository.reference.uri,
                        )
                    },
                    isAvailable = it,
                    text = "Add",
                    running = this.addOperationRunning,
                )
            },
            this.canReindex.mapLoadedData {
                Action(
                    onLaunch = {
                        archiveOperationLaunchers.openReindexOperation(
                            localArchive.primaryRepository.reference.uri,
                        )
                    },
                    isAvailable = it,
                    text = "Reindex changed files",
                )
            },
            this.canCleanupDeletedFiles.mapLoadedData {
                Action(
                    onLaunch = {
                        archiveOperationLaunchers.openDeletedFilesCleanupOperation(
                            localArchive.primaryRepository.reference.uri,
                        )
                    },
                    isAvailable = it,
                    text = "Cleanup deleted files",
                )
            },
            this.canPush.mapLoadedData {
                Action(
                    onLaunch = {
                        archiveOperationLaunchers.pushRepoToAll(
                            localArchive.primaryRepository.reference.uri,
                        )
                    },
                    isAvailable = it && enableUnfinishedFeatures,
                    text = "Push to all",
                )
            },
        )
}
