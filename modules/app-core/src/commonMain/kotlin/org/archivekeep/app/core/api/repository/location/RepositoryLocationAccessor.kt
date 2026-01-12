package org.archivekeep.app.core.api.repository.location

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.persistence.credentials.ContentEncryptionPassword
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.utils.loading.filterLoaded
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
import org.archivekeep.utils.loading.optional.flatMapLatestLoadedData
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

interface RepositoryLocationAccessor {
    /**
     * The current state of available accessor to repository contents.
     *
     * Should use [NeedsUnlock] to signal [OptionalLoadable.NotAvailable] due to pending authentication request.
     */
    val contentsStateFlow: Flow<OptionalLoadable<RepositoryLocationContentsState>>
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<OptionalLoadable<RepositoryLocationContentsState>>.repositoryAccessor(): Flow<RepositoryAccessState> =
    this.flatMapLatestLoadedData {
        if (it is RepositoryLocationContentsState.IsRepositoryLocation) {
            it.repoStateFlow
        } else {
            flowOf(OptionalLoadable.Failed(RuntimeException("Not repository")))
        }
    }

fun Flow<RepositoryAccessState>.autoUnlocker(
    uri: RepositoryURI,
    credentialsStore: CredentialsStore,
) = transformLatest { repoLoadable ->
    when (repoLoadable) {
        is NeedsUnlock -> {
            coroutineScope {
                val onShowLocked = Channel<Unit>(onBufferOverflow = BufferOverflow.DROP_OLDEST)

                launch {
                    autoUnlock(uri, credentialsStore, repoLoadable, onShowLocked)
                }

                launch {
                    delay(300.milliseconds)
                    onShowLocked.send(Unit)
                }

                onShowLocked.receive()

                emit(repoLoadable)
            }
        }

        else -> {
            emit(repoLoadable)
        }
    }
}

private suspend fun FlowCollector<OptionalLoadable<Repo>>.autoUnlock(
    uri: RepositoryURI,
    credentialsStore: CredentialsStore,
    repoLoadable: NeedsUnlock,
    onShowLocked: Channel<Unit>,
) {
    when (val unlockRequest = repoLoadable.unlockRequest) {
        is PasswordRequest -> {
            credentialsStore
                .getRepositorySecretsFlow(uri)
                .conflate()
                .takeWhile { optionalCredentials ->
                    when (optionalCredentials) {
                        is LoadedAvailable -> {
                            optionalCredentials.value[ContentEncryptionPassword]?.let { contentEncryptionPassword ->
                                try {
                                    unlockRequest.providePassword(
                                        contentEncryptionPassword.jsonPrimitive.content,
                                        false,
                                    )

                                    return@takeWhile false
                                } catch (e: Throwable) {
                                    // TODO: expose failure to UI
                                    println("Auto-unlock failed: $e")
                                } finally {
                                    onShowLocked.send(Unit)
                                }
                            }
                        }

                        is OptionalLoadable.Loading -> {}
                        else -> onShowLocked.send(Unit)
                    }

                    true
                }.collect {}
        }

        is UserCredentialsRequest -> {
            try {
                val inMemoryCredentials = credentialsStore.inMemoryCredentials.value[uri]
                if (inMemoryCredentials != null) {
                    unlockRequest.tryOpen(inMemoryCredentials, UnlockOptions(false, false))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                println("Auto-open with in-memory credentials failed: $e")
            }

            try {
                val credentialsFlow = credentialsStore.getRepositoryCredentialsFlow(uri)

                val storedCredentials =
                    credentialsStore.inMemoryCredentials.value[uri] ?: credentialsFlow.firstLoadedOrNullOnErrorOrLocked()

                if (storedCredentials != null) {
                    unlockRequest.tryOpen(storedCredentials, UnlockOptions(false, false))
                }

                emit(repoLoadable)
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
}
