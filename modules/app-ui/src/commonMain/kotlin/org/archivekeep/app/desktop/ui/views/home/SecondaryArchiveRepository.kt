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
import org.archivekeep.app.desktop.ui.utils.combineTexts
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.utils.filesAutoPlural

class SecondaryArchiveRepository(
    val primaryRepositoryURI: RepositoryURI?,
    // TODO: reference only
    val otherRepositoryState: ResolvedRepositoryState,
    val repository: Repository,
) {
    val reference: NamedRepositoryReference = otherRepositoryState.namedReference

    data class State(
        val repo: SecondaryArchiveRepository,
        val localRepoStatus: OptionalLoadable<StatusOperation.Result.Summary>,
        val connectionStatus: Repository.ConnectionState,
        val syncRunning: Boolean,
        val canPushLoadable: OptionalLoadable<Boolean>,
        val canPull: Boolean,
        val syncTexts: OptionalLoadable<List<String>>,
    ) {
        val needsUnlock = connectionStatus is Repository.ConnectionState.ConnectedLocked

        val addTexts =
            localRepoStatus.mapLoadedData {
                if (it.totalNewFiles > 0) {
                    listOf("Uncommitted ${it.totalNewFiles} ${filesAutoPlural(it.totalNewFiles)}")
                } else {
                    emptyList()
                }
            }

        val texts: OptionalLoadable<String> =
            combineTexts(
                addTexts,
                syncTexts,
            ).mapLoadedData { it.joinToString(", ") }

        val isLoading = syncTexts.isLoading || syncRunning || localRepoStatus.isLoading

        val canAdd = localRepoStatus.mapIfLoadedOrNull { it.totalNewFiles > 0 } ?: false
        val canPush = canPushLoadable.mapIfLoadedOrNull { it } ?: false
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
                    (it.missingBaseInOther != 0 || it.relocations > 0) && connectionStatus.isAvailable
                } ?: OptionalLoadable.NotAvailable()

            val canPull =
                syncStatus?.mapIfLoadedOrNull {
                    it.missingOtherInBase != 0 || it.relocations > 0
                } ?: false

            val syncTexts = syncStatus?.mapLoadedData(::textTags) ?: OptionalLoadable.NotAvailable()

            State(
                repo = this,
                connectionStatus = connectionStatus,
                localRepoStatus = localRepoStatus.mapLoadedData { it.summary },
                syncRunning = syncRunning,
                canPushLoadable = canPushLoadable,
                canPull = canPull && connectionStatus.isAvailable,
                syncTexts = syncTexts,
            )
        }.stateIn(scope, SharingStarted.Lazily, initialValue)
    }
}

fun textTags(status: RepoToRepoSync.CompareState): List<String> =
    listOfNotNull(
        if (status.missingBaseInOther > 0) "${status.missingBaseInOther} missing" else null,
        if (status.missingOtherInBase > 0) "${status.missingOtherInBase} extra" else null,
        if (status.relocations == 1) "${status.relocations} relocation" else null,
        if (status.relocations > 1) "${status.relocations} relocations" else null,
    ).ifEmpty {
        listOf("100% synced")
    }
