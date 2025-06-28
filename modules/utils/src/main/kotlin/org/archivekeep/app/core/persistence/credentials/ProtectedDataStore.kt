package org.archivekeep.app.core.persistence.credentials

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.ProtectedLoadableResource

interface ProtectedDataStore<E> {
    val data: Flow<ProtectedLoadableResource<E, Any>>

    suspend fun needsUnlock(): Boolean

    suspend fun updateData(transform: suspend (t: E) -> E): E
}
