package org.archivekeep.app.core.persistence.credentials

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapLoadedData

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(CoreApplicationServiceScope::class)
class CredentialsInProtectedWalletDataStore(
    val datastore: ProtectedDataStore<WalletPO>,
) : CredentialsStore {
    override val inMemoryCredentials = MutableStateFlow(emptyMap<RepositoryURI, BasicAuthCredentials>())

    override fun getRepositorySecretsFlow(uri: RepositoryURI): Flow<OptionalLoadable<Map<RepositorySecretType, JsonElement>>> =
        datastore
            .data
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
    override fun getRepositoryCredentialsFlow(uri: RepositoryURI): Flow<OptionalLoadable<BasicAuthCredentials?>> =
        datastore
            .data
            .mapLoadedData {
                it.repositoryCredentials.firstOrNull { it.uri == uri }?.let {
                    Json.decodeFromString<BasicAuthCredentials>(it.rawCredentials)
                }
            }

    override suspend fun saveRepositoryCredentials(
        repositoryURI: RepositoryURI,
        credentials: BasicAuthCredentials,
    ) {
        updateRepositoryCredentials { old ->
            val oldWithoutCurrentURI = old.filter { oldCredential -> oldCredential.uri != repositoryURI }.toSet()

            oldWithoutCurrentURI +
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
