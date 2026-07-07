package org.archivekeep.app.ui.views.home.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.archivekeep.utils.loading.optional.mapLoadedData

class HomeLocalArchiveSecondaryRepositoryModel(
    val primaryRepositoryURI: RepositoryURI?,
    // TODO: reference only
    val otherRepositoryState: ResolvedRepositoryState,
    val repository: Repository,
) {
    val reference: NamedRepositoryReference = otherRepositoryState.namedReference

    fun stateFlow(
        scope: CoroutineScope,
        repoToRepoSyncService: RepoToRepoSyncService,
    ): StateFlow<HomeLocalArchiveSecondaryRepositoryUiState> {
        val repoToRepoSync =
            primaryRepositoryURI?.let {
                repoToRepoSyncService.getRepoToRepoSync(
                    primaryRepositoryURI,
                    repository.uri,
                )
            }

        val syncStatusFlow = repoToRepoSync?.compareStateFlow?.onStart { emit(OptionalLoadable.Loading) } ?: MutableStateFlow(null)
        val syncRunningFlow = repoToRepoSync?.currentJobFlow?.map { it != null } ?: MutableStateFlow(false)

        val initialValue =
            HomeLocalArchiveSecondaryRepositoryUiState(
                repo = this,
                connectionStatus = otherRepositoryState.connectionState,
                localRepoStatus = OptionalLoadable.Loading,
                syncRunning = false,
                canPushLoadable = OptionalLoadable.Loading,
                canPull = false,
                syncTexts = OptionalLoadable.Loading,
            )

        return combine(
            syncStatusFlow,
            syncRunningFlow,
            repository.localRepoStatus,
        ) { syncStatus, syncRunning, localRepoStatus ->
            val connectionStatus = otherRepositoryState.connectionState

            val canPushLoadable =
                syncStatus?.mapLoadedData {
                    (it.missingBaseInOther != 0 || it.relocations > 0) && connectionStatus.isConnected
                } ?: OptionalLoadable.NotAvailable()

            val canPull =
                syncStatus?.mapIfLoadedOrNull {
                    it.missingOtherInBase != 0 || it.relocations > 0
                } ?: false

            val syncTexts = syncStatus?.mapLoadedData(::textTags) ?: OptionalLoadable.NotAvailable()

            HomeLocalArchiveSecondaryRepositoryUiState(
                repo = this,
                connectionStatus = connectionStatus,
                localRepoStatus = localRepoStatus.mapLoadedData { it.summary },
                syncRunning = syncRunning,
                canPushLoadable = canPushLoadable,
                canPull = canPull && connectionStatus.isConnected,
                syncTexts = syncTexts,
            )
        }.stateIn(scope, SharingStarted.Lazily, initialValue)
    }
}

private fun textTags(status: RepoToRepoSync.CompareState): List<String> =
    listOfNotNull(
        if (status.missingBaseInOther > 0) "${status.missingBaseInOther} missing" else null,
        if (status.missingOtherInBase > 0) "${status.missingOtherInBase} extra" else null,
        if (status.relocations == 1) "${status.relocations} relocation" else null,
        if (status.relocations > 1) "${status.relocations} relocations" else null,
    ).ifEmpty {
        listOf("100% synced")
    }
