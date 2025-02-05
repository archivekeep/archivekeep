package org.archivekeep.app.core.persistence.credentials

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.core.repo.remote.grpc.BasicAuthCredentials

class CredentialsInProtectedDataStore(
    val datastore: ProtectedDataStore<Credentials>,
) : CredentialsStore {
    override fun getRepositoryCredentialsFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<BasicAuthCredentials?, Any>> =
        datastore.data
            .map {
                when (it) {
                    is ProtectedLoadableResource.Failed ->
                        it
                    is ProtectedLoadableResource.Loaded ->
                        ProtectedLoadableResource.Loaded(
                            it.value.repositoryCredentials.firstOrNull { it.uri == uri }?.let {
                                Json.decodeFromString<BasicAuthCredentials>(it.rawCredentials)
                            },
                        )
                    is ProtectedLoadableResource.Loading ->
                        it
                    is ProtectedLoadableResource.PendingAuthentication ->
                        it
                }
            }

    override suspend fun rememberRepositoryCredentials(
        repositoryURI: RepositoryURI,
        credentials: BasicAuthCredentials,
    ) {
        updateRepositoryCredentials { old ->
            old +
                setOf(
                    PersistedRepositoryCredentials(
                        repositoryURI,
                        Json.encodeToString(credentials),
                    ),
                )
        }
    }

    private suspend fun updateRepositoryCredentials(fn: (old: Set<PersistedRepositoryCredentials>) -> Set<PersistedRepositoryCredentials>) {
        datastore.updateData { data ->
            data.copy(
                repositoryCredentials = fn(data.repositoryCredentials),
            )
        }
    }

    @Serializable
    data class PersistedRepositoryCredentials(
        val uri: RepositoryURI,
        val rawCredentials: String,
    )
}
