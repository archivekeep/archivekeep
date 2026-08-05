package org.archivekeep.app.ui.views.home.model

import androidx.compose.runtime.Composable
import compose.icons.TablerIcons
import compose.icons.tablericons.Folder
import compose.icons.tablericons.LockOpen
import compose.icons.tablericons.Plus
import compose.icons.tablericons.RefreshAlert
import compose.icons.tablericons.Trash
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.needsUnlockLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.feature.repository.withRepositoryOpener
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.files.driver.filesystem.files.FilesystemWorkingRepository
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.optional.mapToLoadable
import kotlin.io.path.absolutePathString

class RepositoryOperationalState(
    repositoryAccessState: RepositoryAccessState,
    val isAssociated: Boolean,
    localRepoStatus: OptionalLoadable<StatusOperation.Result.Summary>,
) {
    val canAdd = localRepoStatus.mapLoadedData { it.totalNewFiles > 0 }.mapToLoadable(false)
    val canReindex = localRepoStatus.mapLoadedData { it.totalModifiedIndexedFiles > 0 }.mapToLoadable(false)
    val canCleanupDeletedFiles = localRepoStatus.mapLoadedData { it.totalMissingFiles > 0 }.mapToLoadable(false)

    val needsUnlock = repositoryAccessState.needsUnlockLoadable()

    val filesystemRepo = repositoryAccessState.mapLoadedData { it as? FilesystemWorkingRepository }.mapToLoadable(null)

    @Composable
    fun actions(
        launchers: ArchiveOperationLaunchers,
        uri: RepositoryURI,
    ): RepositoryBaseUiActions =
        object : RepositoryBaseUiActions {
            override val open: Loadable<Action> =
                withRepositoryOpener(uri) {
                    Action(
                        icon = TablerIcons.Folder,
                        title = "Open",
                        isPending = false,
                        isAvailable = openRepository != null,
                        onLaunch = openRepository ?: {},
                    )
                }

            override val unlock =
                needsUnlock.mapLoadedData {
                    Action(
                        TablerIcons.LockOpen,
                        "Unlock",
                        isPending = it,
                        isAvailable = it,
                        onLaunch = {
                            launchers.unlockRepository(uri, null)
                        },
                    )
                }

            override val add =
                canAdd.mapLoadedData {
                    Action(
                        TablerIcons.Plus,
                        "Add new files",
                        isAvailable = it,
                        onLaunch = {
                            launchers.openIndexUpdateOperation(uri)
                        },
                    )
                }

            override val cleanupFiles =
                canCleanupDeletedFiles.mapLoadedData {
                    Action(
                        TablerIcons.Trash,
                        "Cleanup deleted files",
                        isAvailable = it,
                        onLaunch = {
                            launchers.openDeletedFilesCleanupOperation(uri)
                        },
                    )
                }

            override val reindex =
                canReindex.mapLoadedData {
                    Action(
                        TablerIcons.RefreshAlert,
                        "Reindex changed files",
                        isAvailable = it,
                        onLaunch = {
                            launchers.openReindexOperation(uri)
                        },
                    )
                }

            override val associate =
                Loadable.Loaded(
                    Action(
                        title = "Associate",
                        isPending = !isAssociated,
                        isAvailable = !isAssociated,
                        onLaunch = {
                            launchers.openAssociateRepository(uri)
                        },
                    ),
                )

            override val unassociate =
                Loadable.Loaded(
                    Action(
                        title = "Unassociate",
                        isPending = false,
                        isAvailable = isAssociated,
                        onLaunch = {
                            launchers.openUnassociateRepository(uri)
                        },
                    ),
                )

            override val forget: Loadable<Action> =
                Loadable.Loaded(
                    Action(
                        null,
                        "Forget",
                        isPending = false,
                        onLaunch = {
                            launchers.openForgetRepository(uri)
                        },
                    ),
                )
            override val deinitialize =
                filesystemRepo.mapLoadedData {
                    Action(
                        null,
                        "Deinitialize",
                        isPending = false,
                        isAvailable = it != null,
                        onLaunch = {
                            launchers.openDeinitializeFilesystemRepository(uri, it!!.root.absolutePathString())
                        },
                    )
                }
        }
}
