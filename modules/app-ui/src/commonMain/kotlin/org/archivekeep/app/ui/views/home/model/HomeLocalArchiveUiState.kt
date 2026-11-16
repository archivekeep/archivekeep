package org.archivekeep.app.ui.views.home.model

import androidx.compose.runtime.Composable
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.enableUnfinishedFeatures
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.home_action_add_and_push
import org.archivekeep.ui.resources.home_action_push_to_all
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.jetbrains.compose.resources.stringResource

data class HomeLocalArchiveUiState(
    val repositoryAccessState: RepositoryAccessState,
    val localRepoStatus: OptionalLoadable<StatusOperation.Result.Summary>,
    val canPush: Loadable<Boolean>,
    val anySecondaryAvailable: Boolean,
    val loading: Boolean,
    val indexStatus: OptionalLoadable<Pair<Int, Int>>,
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
        val addAndPushTitle = stringResource(Res.string.home_action_add_and_push)
        val pushToAllTitle = stringResource(Res.string.home_action_push_to_all)

        return object : HomeLocalArchiveUiActions, RepositoryBaseUiActions by ra {
            override val addPush =
                canAddPush.mapLoadedData {
                    Action(
                        onLaunch = {
                            archiveOperationLaunchers.openAddAndPushOperation(
                                localArchive.primaryRepository.reference.uri,
                            )
                        },
                        title = addAndPushTitle,
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
                        title = pushToAllTitle,
                    )
                }
        }
    }
}
