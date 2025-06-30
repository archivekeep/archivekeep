package org.archivekeep.app.core.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformWhile
import org.archivekeep.app.core.utils.exceptions.RepositoryLockedException
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.ProtectedLoadableResource
import org.archivekeep.utils.loading.mapAsLoadable

class ProtectedProviderOfRepositoryAccessor(
    private val uri: RepositoryURI,
    private val protectedAccessorFlow: Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>>,
) {
    val needsUnlock =
        flow {
            emitAll(
                // keep strong reference to this
                this@ProtectedProviderOfRepositoryAccessor
                    .protectedAccessorFlow
                    .map { it is ProtectedLoadableResource.PendingAuthentication },
            )
        }

    val optionalAccessorFlow =
        flow {
            emitAll(
                // keep strong reference to this
                this@ProtectedProviderOfRepositoryAccessor
                    .protectedAccessorFlow
                    .map { it.asOptionalLoadable() },
            )
        }

    val accessorFlow =
        flow {
            emitAll(
                // keep strong reference to this
                this@ProtectedProviderOfRepositoryAccessor
                    .protectedAccessorFlow
                    .mapAsLoadable(),
            )
        }

    val statusFlow =
        flow {
            emitAll(
                // keep strong reference to this
                this@ProtectedProviderOfRepositoryAccessor
                    .protectedAccessorFlow
                    .map { it.asStatus() }
                    .onStart { emit(RepositoryConnectionState.Disconnected) },
            )
        }

    suspend fun unlock(
        basicCredentials: BasicAuthCredentials,
        options: UnlockOptions,
    ) {
        val state: ProtectedLoadableResource<Repo, RepoAuthRequest> =
            protectedAccessorFlow
                .transformWhile {
                    if (it is ProtectedLoadableResource.Loading) {
                        true
                    } else {
                        emit(it)
                        false
                    }
                }.first()

        when (state) {
            is ProtectedLoadableResource.Loading ->
                throw RuntimeException("Shouldn't happen")

            is ProtectedLoadableResource.Failed -> {
                throw RuntimeException("Access failed")
            }

            is ProtectedLoadableResource.Loaded -> {
                throw RuntimeException("Doesn't need unlock")
            }

            is ProtectedLoadableResource.PendingAuthentication -> {
                state.authenticationRequest.tryOpen(basicCredentials, options)
            }
        }
    }

    suspend fun requireLoadedAccessor() =
        protectedAccessorFlow
            .transform {
                when (it) {
                    is ProtectedLoadableResource.Failed -> throw it.throwable
                    is ProtectedLoadableResource.Loaded -> emit(it.value)
                    ProtectedLoadableResource.Loading -> {}
                    is ProtectedLoadableResource.PendingAuthentication -> throw RepositoryLockedException(uri)
                }
            }.first()
}
