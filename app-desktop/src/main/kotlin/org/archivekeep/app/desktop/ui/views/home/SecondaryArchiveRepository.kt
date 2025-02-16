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
import org.archivekeep.app.core.operations.derived.RepoToRepoSync
import org.archivekeep.app.core.operations.derived.SyncService
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
        val canPush: Boolean,
        val canPull: Boolean,
        val texts: OptionalLoadable<String>,
    ) {
        val needsUnlock = connectionStatus is Repository.ConnectionState.ConnectedLocked

        val isLoading = texts.isLoading || syncRunning
    }

    fun stateFlow(
        scope: CoroutineScope,
        syncService: SyncService,
    ): StateFlow<State> {
        val repoToRepoSync =
            primaryRepositoryURI?.let {
                syncService.getRepoToRepoSync(
                    primaryRepositoryURI,
                    repository.uri,
                )
            }

        val syncStatusFlow = repoToRepoSync?.stateFlow?.onStart { emit(OptionalLoadable.Loading) } ?: MutableStateFlow(null)
        val syncRunningFlow = repoToRepoSync?.currentlyRunningOperationFlow?.map { it != null } ?: MutableStateFlow(false)

        val initialValue =
            State(
                repo = this,
                connectionStatus = otherRepositoryState.connectionState,
                syncRunning = false,
                canPush = false,
                canPull = false,
                texts = OptionalLoadable.Loading,
            )

        return combine(
            syncStatusFlow,
            syncRunningFlow,
        ) { syncStatus, syncRunning ->
            val connectionStatus = otherRepositoryState.connectionState

            val canPush =
                syncStatus?.mapIfLoadedOrNull {
                    it.missingBaseInOther != 0
                } ?: false

            val canPull =
                syncStatus?.mapIfLoadedOrNull {
                    it.missingOtherInBase != 0
                } ?: false

            val texts = syncStatus?.mapLoadedData(::textTags) ?: OptionalLoadable.NotAvailable()

            State(
                repo = this,
                connectionStatus = connectionStatus,
                syncRunning = syncRunning,
                canPush = canPush && connectionStatus.isAvailable,
                canPull = canPull && connectionStatus.isAvailable,
                texts = texts,
            )
        }.stateIn(scope, SharingStarted.Lazily, initialValue)
    }
}

fun textTags(syncStatuss: RepoToRepoSync.State): String {
    val outOfSyncParts =
        listOfNotNull(
            if (syncStatuss.missingBaseInOther > 0) "${syncStatuss.missingBaseInOther} missing" else null,
            if (syncStatuss.missingOtherInBase > 0) "${syncStatuss.missingOtherInBase} extra" else null,
        )

    return if (outOfSyncParts.isNotEmpty()) {
        outOfSyncParts.joinToString(", ")
    } else {
        "100% synced"
    }
}
