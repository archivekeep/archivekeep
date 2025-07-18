package org.archivekeep.app.core.persistence.credentials

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

/**
 * Persistence object for Wallet data structure at rest.
 */
@Serializable
data class WalletPO(
    @Deprecated("Switch to repository secrets")
    val repositoryCredentials: Set<CredentialsInProtectedWalletDataStore.PersistedRepositoryCredentials>,
    @EncodeDefault
    val repositorySecrets: Map<RepositoryURI, Map<RepositorySecretType, JsonElement>> = emptyMap(),
)
