package org.archivekeep.app.core.persistence.credentials

import kotlinx.serialization.Serializable

@Serializable
data class Credentials(
    val repositoryCredentials: Set<CredentialsInProtectedDataStore.PersistedRepositoryCredentials>,
)
