package org.archivekeep.app.core.persistence.credentials

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore
import org.archivekeep.utils.loading.ProtectedLoadableResource
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapLoadedData

class CredentialsInProtectedWalletDataStore(
    val datastore: ProtectedDataStore<WalletPO>,
) : CredentialsStore {
    override fun getRepositorySecretsFlow(uri: RepositoryURI): Flow<OptionalLoadable<Map<RepositorySecretType, JsonElement>>> =
        datastore
            .dataOptional
            .mapLoadedData {
                it.repositorySecrets[uri] ?: emptyMap()
            }

    override suspend fun saveRepositorySecret(
        repositoryURI: RepositoryURI,
        secretType: RepositorySecretType,
        secret: JsonElement,
    ) {
        datastore.updateData {
            it.copy(
                repositorySecrets =
                    it.repositorySecrets +
                        mapOf(
                            repositoryURI to (it.repositorySecrets[repositoryURI] ?: emptyMap()) + mapOf(secretType to secret),
                        ),
            )
        }
    }

    @Deprecated("Switch to getRepositorySecrets")
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

    override suspend fun saveRepositoryCredentials(
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
