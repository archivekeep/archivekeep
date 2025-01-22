package org.archivekeep.app.core.persistence.credentials

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.ProtectedLoadableResource

interface ProtectedDataStore<T> {
    val data: Flow<ProtectedLoadableResource<T, Any>>

    suspend fun updateData(transform: suspend (t: T) -> T): T
}
