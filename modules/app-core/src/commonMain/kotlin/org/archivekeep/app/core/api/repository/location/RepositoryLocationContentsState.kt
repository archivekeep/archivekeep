package org.archivekeep.app.core.api.repository.location

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.domain.storages.RepositoryAccessState

sealed interface RepositoryLocationContentsState {
    interface IsRepositoryLocation : RepositoryLocationContentsState {
        val repoStateFlow: Flow<RepositoryAccessState>
    }

    class LocationCanBeInitialized(
        val initializeAsPlain: (suspend (permanentPreserve: Boolean) -> Unit)?,
        val initializeAsE2EEPasswordProtected: (suspend (password: String, permanentPreserve: Boolean) -> Unit)?,
    ) : RepositoryLocationContentsState
}
