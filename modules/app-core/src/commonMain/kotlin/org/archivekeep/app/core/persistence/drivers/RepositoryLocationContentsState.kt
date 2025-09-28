package org.archivekeep.app.core.persistence.drivers

sealed interface RepositoryLocationContentsState {
    interface IsRepositoryLocation : RepositoryLocationContentsState {
        suspend fun preserveCredentials(
            // TODO: rework - preserve sessions/tokens vs plain credentials
            permanentPreserve: Boolean,
        )
    }

    class LocationCanBeInitialized(
        val initializeAsPlain: (suspend (permanentPreserve: Boolean) -> Unit)?,
        val initializeAsE2EEPasswordProtected: (suspend (password: String, permanentPreserve: Boolean) -> Unit)?,
    ) : RepositoryLocationContentsState
}
