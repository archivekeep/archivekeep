package org.archivekeep.utils.datastore.passwordprotected

import kotlinx.coroutines.flow.Flow
import org.archivekeep.utils.loading.ProtectedLoadableResource

interface ProtectedDataStore<E> {
    val data: Flow<ProtectedLoadableResource<E, Any>>

    suspend fun needsUnlock(): Boolean

    suspend fun updateData(transform: suspend (t: E) -> E): E
}
