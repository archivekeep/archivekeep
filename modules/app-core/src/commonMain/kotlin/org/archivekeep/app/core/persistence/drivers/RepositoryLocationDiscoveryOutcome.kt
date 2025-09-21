package org.archivekeep.app.core.persistence.drivers

sealed interface RepositoryLocationDiscoveryOutcome {
    data object IsRepositoryLocation : RepositoryLocationDiscoveryOutcome

    class LocationCanBeInitialized(
        val initializeAsPlain: (suspend () -> Unit)?,
        val initializeAsE2EEPasswordProtected: (suspend (password: String) -> Unit)?,
    ) : RepositoryLocationDiscoveryOutcome
}
