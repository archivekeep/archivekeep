package org.archivekeep.app.core.persistence.credentials

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonElement
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.archivekeep.utils.loading.ProtectedLoadableResource
import org.archivekeep.utils.loading.optional.OptionalLoadable

interface CredentialsStore {
    val inMemoryCredentials: MutableStateFlow<Map<RepositoryURI, BasicAuthCredentials>>

    fun getRepositorySecretsFlow(uri: RepositoryURI): Flow<OptionalLoadable<Map<RepositorySecretType, JsonElement>>>

    suspend fun saveRepositorySecret(
        repositoryURI: RepositoryURI,
        secretType: RepositorySecretType,
        secret: JsonElement,
    )

    @Deprecated("Switch to getRepositorySecrets")
    fun getRepositoryCredentialsFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<BasicAuthCredentials?, Any>>

    @Deprecated("Switch to saveRepositorySecret")
    suspend fun saveRepositoryCredentials(
        repositoryURI: RepositoryURI,
        credentials: BasicAuthCredentials,
    )
}
