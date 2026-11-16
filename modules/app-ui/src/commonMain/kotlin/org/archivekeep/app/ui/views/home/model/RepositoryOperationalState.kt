package org.archivekeep.app.ui.views.home.model

import androidx.compose.runtime.Composable
import compose.icons.TablerIcons
import compose.icons.tablericons.Folder
import compose.icons.tablericons.InfoCircle
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
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_add_new_files
import org.archivekeep.ui.resources.common_associate
import org.archivekeep.ui.resources.common_cleanup_deleted_files
import org.archivekeep.ui.resources.common_deinitialize
import org.archivekeep.ui.resources.common_details
import org.archivekeep.ui.resources.common_forget
import org.archivekeep.ui.resources.common_open
import org.archivekeep.ui.resources.common_reindex_changed_files
import org.archivekeep.ui.resources.common_unassociate
import org.archivekeep.ui.resources.common_verify_contents
import org.archivekeep.ui.resources.common_unlock
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.optional.mapToLoadable
import org.jetbrains.compose.resources.stringResource

class RepositoryOperationalState(
    repositoryAccessState: RepositoryAccessState,
    val isAssociated: Boolean,
    localRepoStatus: OptionalLoadable<StatusOperation.Result.Summary>,
) {
    val canAdd = localRepoStatus.mapLoadedData { it.canAdd }.mapToLoadable(false)
    val canReindex = localRepoStatus.mapLoadedData { it.totalModifiedIndexedFiles > 0 }.mapToLoadable(false)
    val canCleanupDeletedFiles = localRepoStatus.mapLoadedData { it.totalMissingFiles > 0 }.mapToLoadable(false)

    val needsUnlock = repositoryAccessState.needsUnlockLoadable()

    val filesystemRepo = repositoryAccessState.mapLoadedData { it as? FilesystemWorkingRepository }.mapToLoadable(null)

    @Composable
    fun actions(
        launchers: ArchiveOperationLaunchers,
        uri: RepositoryURI,
    ): RepositoryBaseUiActions {
        val commonOpen = stringResource(Res.string.common_open)
        val commonDetails = stringResource(Res.string.common_details)
        val commonUnlock = stringResource(Res.string.common_unlock)
        val commonAddNewFiles = stringResource(Res.string.common_add_new_files)
        val commonCleanupDeletedFiles = stringResource(Res.string.common_cleanup_deleted_files)
        val commonReindexChangedFiles = stringResource(Res.string.common_reindex_changed_files)
        val commonVerifyContents = stringResource(Res.string.common_verify_contents)
        val commonAssociate = stringResource(Res.string.common_associate)
        val commonUnassociate = stringResource(Res.string.common_unassociate)
        val commonForget = stringResource(Res.string.common_forget)
        val commonDeinitialize = stringResource(Res.string.common_deinitialize)

        return object : RepositoryBaseUiActions {
            override val open: Loadable<Action> =
                withRepositoryOpener(uri) {
                    Action(
                        icon = TablerIcons.Folder,
                        title = commonOpen,
                        isPending = false,
                        isAvailable = openRepository != null,
                        onLaunch = openRepository ?: {},
                    )
                }

            override val details: Loadable<Action> =
                Loadable.Loaded(
                    Action(
                        icon = TablerIcons.InfoCircle,
                        title = commonDetails,
                        isPending = false,
                        onLaunch = {
                            launchers.openRepositoryDetails(uri)
                        },
                    ),
                )

            override val unlock =
                needsUnlock.mapLoadedData {
                    Action(
                        TablerIcons.LockOpen,
                        commonUnlock,
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
                        commonAddNewFiles,
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
                        commonCleanupDeletedFiles,
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
                        commonReindexChangedFiles,
                        isAvailable = it,
                        onLaunch = {
                            launchers.openReindexOperation(uri)
                        },
                    )
                }

            override val verify: Loadable<Action> =
                Loadable.Loaded(
                    Action(
                        null,
                        commonVerifyContents,
                        isPending = false,
                        onLaunch = {
                            launchers.openVerifyOperation(uri)
                        },
                    ),
                )

            override val associate =
                Loadable.Loaded(
                    Action(
                        title = commonAssociate,
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
                        title = commonUnassociate,
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
                        commonForget,
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
                        commonDeinitialize,
                        isPending = false,
                        isAvailable = it != null,
                        onLaunch = {
                            launchers.openDeinitializeFilesystemRepositoryByUri(uri)
                        },
                    )
                }
        }
    }
}
