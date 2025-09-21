package org.archivekeep.app.core.persistence.drivers

interface RepositoryLocationDiscoveryForAdd {
    fun asRepositoryLocationDiscoveryOutcome(): RepositoryLocationDiscoveryOutcome

    suspend fun preserveCredentialss(
        // TODO: rework - preserve sessions/tokens vs plain credentials
        rememberCredentials: Boolean,
    )
}
