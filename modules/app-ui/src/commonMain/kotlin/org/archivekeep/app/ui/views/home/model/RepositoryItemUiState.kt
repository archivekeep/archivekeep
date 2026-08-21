package org.archivekeep.app.ui.views.home.model

import androidx.compose.runtime.Composable
import compose.icons.TablerIcons
import compose.icons.tablericons.Download
import compose.icons.tablericons.Upload
import org.archivekeep.app.core.domain.repositories.RepositoryConnectionState
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.app.ui.utils.combineTexts
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.isLoading
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.optional.mapToLoadable
import org.archivekeep.utils.text.filesAutoPlural

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
    val addTexts =
        localRepoStatus.mapLoadedData {
            if (it.totalNewFiles > 0) {
                listOf("Uncommitted ${filesAutoPlural(it.totalNewFiles)}")
            } else {
                emptyList()
            }
        }

    val texts: OptionalLoadable<String> =
        combineTexts(
            OptionalLoadable.LoadedAvailable(
                if (connectionStatus.isLocked) {
                    listOf("Locked")
                } else if (!connectionStatus.isAccessible) {
                    listOf("Disconnected")
                } else {
                    emptyList()
                },
            ),
            addTexts,
            syncTexts,
        ).mapLoadedData { it.joinToString(", ") }

    val isLoading = syncTexts.isLoading || syncRunning || localRepoStatus.isLoading

    val repositoryOperationalState = RepositoryOperationalState(repositoryAccessState, repo.otherRepositoryState.associationId != null, localRepoStatus)

    val canPush = canPushLoadable.mapIfLoadedOrNull { it } ?: false
    val canPull = canPullLoadable.mapIfLoadedOrNull { it } ?: false

    @Composable
    fun actions(launchers: ArchiveOperationLaunchers): RepositoryItemUiActions {
        val ra = repositoryOperationalState.actions(launchers, repo.repository.uri)

        return object : RepositoryItemUiActions, RepositoryBaseUiActions by ra {
            override val push =
                canPushLoadable.mapToLoadable(false).mapLoadedData {
                    Action(
                        TablerIcons.Upload,
                        "Push",
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
                        "Pull",
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
