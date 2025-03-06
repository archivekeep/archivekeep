package org.archivekeep.app.desktop.ui.views.home

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
import org.archivekeep.app.core.operations.sync.RepoToRepoSync
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncService
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.isLoading
import org.archivekeep.app.core.utils.generics.mapIfLoadedOrNull
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

class SecondaryArchiveRepository(
    val primaryRepositoryURI: RepositoryURI?,
    // TODO: reference only
    val otherRepositoryState: ResolvedRepositoryState,
    val repository: Repository,
) {
    val reference: NamedRepositoryReference = otherRepositoryState.namedReference

    data class State(
        val repo: SecondaryArchiveRepository,
        val connectionStatus: Repository.ConnectionState,
        val syncRunning: Boolean,
        val canPushLoadable: OptionalLoadable<Boolean>,
        val canPull: Boolean,
        val texts: OptionalLoadable<String>,
    ) {
        val needsUnlock = connectionStatus is Repository.ConnectionState.ConnectedLocked

        val isLoading = texts.isLoading || syncRunning

        val canPush: Boolean = canPushLoadable.mapIfLoadedOrNull { it } ?: false
    }

    fun stateFlow(
        scope: CoroutineScope,
        repoToRepoSyncService: RepoToRepoSyncService,
    ): StateFlow<State> {
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
            State(
                repo = this,
                connectionStatus = otherRepositoryState.connectionState,
                syncRunning = false,
                canPushLoadable = OptionalLoadable.Loading,
                canPull = false,
                texts = OptionalLoadable.Loading,
            )

        return combine(
            syncStatusFlow,
            syncRunningFlow,
        ) { syncStatus, syncRunning ->
            val connectionStatus = otherRepositoryState.connectionState

            val canPushLoadable =
                syncStatus?.mapLoadedData {
                    (it.missingBaseInOther != 0 || it.relocations > 0) && connectionStatus.isAvailable
                } ?: OptionalLoadable.NotAvailable()

            val canPull =
                syncStatus?.mapIfLoadedOrNull {
                    it.missingOtherInBase != 0 || it.relocations > 0
                } ?: false

            val texts = syncStatus?.mapLoadedData(::textTags) ?: OptionalLoadable.NotAvailable()

            State(
                repo = this,
                connectionStatus = connectionStatus,
                syncRunning = syncRunning,
                canPushLoadable = canPushLoadable,
                canPull = canPull && connectionStatus.isAvailable,
                texts = texts,
            )
        }.stateIn(scope, SharingStarted.Lazily, initialValue)
    }
}

fun textTags(status: RepoToRepoSync.CompareState): String {
    val outOfSyncParts =
        listOfNotNull(
            if (status.missingBaseInOther > 0) "${status.missingBaseInOther} missing" else null,
            if (status.missingOtherInBase > 0) "${status.missingOtherInBase} extra" else null,
            if (status.relocations == 1) "${status.relocations} relocation" else null,
            if (status.relocations > 1) "${status.relocations} relocations" else null,
        )

    return if (outOfSyncParts.isNotEmpty()) {
        outOfSyncParts.joinToString(", ")
    } else {
        "100% synced"
    }
}
