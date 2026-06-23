package org.archivekeep.utils.datastore.passwordprotected

import kotlinx.coroutines.flow.Flow
import org.archivekeep.utils.loading.optional.OptionalLoadable

interface ProtectedDataStore<E> {
    val data: Flow<OptionalLoadable<E>>

    suspend fun needsUnlock(): Boolean

    suspend fun updateData(transform: suspend (t: E) -> E): E
}
