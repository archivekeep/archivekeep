package org.archivekeep.app.ui.views.home.model

import androidx.compose.runtime.Composable
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.enableUnfinishedFeatures
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable

data class HomeLocalArchiveUiState(
    val repositoryAccessState: RepositoryAccessState,
    val localRepoStatus: OptionalLoadable<StatusOperation.Result.Summary>,
    val canPush: Loadable<Boolean>,
    val anySecondaryAvailable: Boolean,
    val loading: Boolean,
    val indexStatusText: OptionalLoadable<String>,
    val addPushOperationRunning: Boolean,
    val addOperationRunning: Boolean,
    val isAssociated: Boolean,
) {
    val repositoryOperationalState = RepositoryOperationalState(repositoryAccessState, isAssociated, localRepoStatus)

    val canAddPush =
        if (addPushOperationRunning) {
            Loadable.Loaded(true)
        } else {
            repositoryOperationalState.canAdd.mapLoadedData { it && anySecondaryAvailable }
        }

    @Composable
    fun actions(
        archiveOperationLaunchers: ArchiveOperationLaunchers,
        localArchive: HomeLocalArchiveModel,
    ): HomeLocalArchiveUiActions {
        val ra = repositoryOperationalState.actions(archiveOperationLaunchers, localArchive.primaryRepository.reference.uri)

        return object : HomeLocalArchiveUiActions, RepositoryBaseUiActions by ra {
            override val addPush =
                canAddPush.mapLoadedData {
                    Action(
                        onLaunch = {
                            archiveOperationLaunchers.openAddAndPushOperation(
                                localArchive.primaryRepository.reference.uri,
                            )
                        },
                        title = "Add and push",
                        isPending = it,
                        running = addPushOperationRunning,
                    )
                }

            override val push =
                canPush.mapLoadedData {
                    Action(
                        onLaunch = {
                            archiveOperationLaunchers.pushRepoToAll(
                                localArchive.primaryRepository.reference.uri,
                            )
                        },
                        isPending = it && enableUnfinishedFeatures,
                        isAvailable = it && enableUnfinishedFeatures,
                        title = "Push to all",
                    )
                }
        }
    }
}
