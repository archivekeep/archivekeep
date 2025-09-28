package org.archivekeep.app.core.persistence.drivers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.transformLatest
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.filterLoaded
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.optional.OptionalLoadable
import kotlin.coroutines.cancellation.CancellationException

interface RepositoryLocationAccessor {
    val locationContents: Flow<OptionalLoadable<RepositoryLocationContentsState>>

    /**
     * The current state of available accessor to repository contents.
     *
     * Should use [NeedsUnlock] to signal [OptionalLoadable.NotAvailable] due to pending authentication request.
     */
    val repositoryAccessor: Flow<RepositoryAccessState>

    // TODO: move the responsibility to manager/wrapper
    val autoUnlockRepositoryAccessor: Flow<RepositoryAccessState>
}

fun Flow<RepositoryAccessState>.autoUnlocker(
    uri: RepositoryURI,
    inMemoryCredentialsStore: StateFlow<Map<RepositoryURI, BasicAuthCredentials>>,
    credentialsStore: CredentialsStore,
) = transformLatest {
    when (it) {
        is NeedsUnlock -> {
            when (val unlockRequest = it.unlockRequest) {
                is RepoAuthRequest -> {
                    val credentialsFlow = credentialsStore.getRepositoryCredentialsFlow(uri)

                    try {
                        val storedCredentials =
                            inMemoryCredentialsStore.value[uri] ?: credentialsFlow.firstLoadedOrNullOnErrorOrLocked()
                        if (storedCredentials != null) {
                            unlockRequest.tryOpen(storedCredentials, UnlockOptions(false, false))
                        }

                        emit(it)
                        credentialsFlow
                            .filterLoaded()
                            .collect {
                                it.value?.let { storedCredentials ->
                                    unlockRequest.tryOpen(storedCredentials, UnlockOptions(false, false))
                                }
                            }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        println("Auto-open failed: $e")
                    }
                }
            }
            emit(it)
        }

        else -> {
            emit(it)
        }
    }
}
