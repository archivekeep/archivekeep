package org.archivekeep.app.ui.views.home.model

import androidx.compose.runtime.Composable
import compose.icons.TablerIcons
import compose.icons.tablericons.Download
import compose.icons.tablericons.Upload
import org.archivekeep.app.core.domain.repositories.RepositoryConnectionState
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.app.ui.utils.filesAutoPluralTranslated
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_disconnected
import org.archivekeep.ui.resources.common_locked
import org.archivekeep.ui.resources.common_pull
import org.archivekeep.ui.resources.common_push
import org.archivekeep.ui.resources.common_unindexed_files
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.isLoading
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.optional.mapToLoadable
import org.jetbrains.compose.resources.stringResource

data class RepositoryItemUiState(
    val repo: RepositoryItemModel,
    val repositoryAccessState: RepositoryAccessState,
    val localRepoStatus: OptionalLoadable<StatusOperation.Result.Summary>,
    val connectionStatus: RepositoryConnectionState,
    val syncRunning: Boolean,
    val canPushLoadable: OptionalLoadable<Boolean>,
    val canPullLoadable: OptionalLoadable<Boolean>,
    val syncTexts: OptionalLoadable<List<String>>,
) {
    val unindexedFileCount: OptionalLoadable<Int> =
        localRepoStatus.mapLoadedData { it.totalNewFiles }

    val isLoading = syncTexts.isLoading || syncRunning || localRepoStatus.isLoading

    val repositoryOperationalState = RepositoryOperationalState(repositoryAccessState, repo.otherRepositoryState.associationId != null, localRepoStatus)

    val canPush = canPushLoadable.mapIfLoadedOrNull { it } ?: false
    val canPull = canPullLoadable.mapIfLoadedOrNull { it } ?: false

    @Composable
    fun textsTranslated(): OptionalLoadable<String> {
        val locked = stringResource(Res.string.common_locked)
        val disconnected = stringResource(Res.string.common_disconnected)
        val unindexed = stringResource(Res.string.common_unindexed_files)

        val statusParts = mutableListOf<String>()
        if (connectionStatus.isLocked) {
            statusParts.add(locked)
        } else if (!connectionStatus.isAccessible) {
            statusParts.add(disconnected)
        }

        val unindexedCount = unindexedFileCount.mapIfLoadedOrNull { it } ?: 0
        if (unindexedCount > 0) {
            statusParts.add(String.format(unindexed, filesAutoPluralTranslated(unindexedCount)))
        }

        syncTexts.mapLoadedData { texts ->
            statusParts.addAll(texts)
        }

        return if (statusParts.isEmpty()) {
            OptionalLoadable.LoadedAvailable("")
        } else {
            OptionalLoadable.LoadedAvailable(statusParts.joinToString(", "))
        }
    }

    @Composable
    fun actions(launchers: ArchiveOperationLaunchers): RepositoryItemUiActions {
        val ra = repositoryOperationalState.actions(launchers, repo.repository.uri)
        val commonPush = stringResource(Res.string.common_push)
        val commonPull = stringResource(Res.string.common_pull)

        return object : RepositoryItemUiActions, RepositoryBaseUiActions by ra {
            override val push =
                canPushLoadable.mapToLoadable(false).mapLoadedData {
                    Action(
                        TablerIcons.Upload,
                        commonPush,
                        isPending = it,
                        isAvailable = it,
                        onLaunch = {
                            launchers.pushToRepo(
                                repo.repository.uri,
                                repo.primaryRepositoryURI!!,
                            )
                        },
                    )
                }

            override val pull =
                canPullLoadable.mapToLoadable(false).mapLoadedData {
                    Action(
                        TablerIcons.Download,
                        commonPull,
                        isPending = it,
                        isAvailable = it,
                        onLaunch = {
                            launchers.pullFromRepo(
                                repo.repository.uri,
                                repo.primaryRepositoryURI!!,
                            )
                        },
                    )
                }
        }
    }
}
