package org.archivekeep.utils.datastore.passwordprotected

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.archivekeep.utils.loading.ProtectedLoadableResource
import org.archivekeep.utils.loading.optional.OptionalLoadable

interface ProtectedDataStore<E> {
    @Deprecated(
        message = "Drop ProtectedLoadableResource",
        replaceWith = ReplaceWith("dataOptional"),
    )
    val data: Flow<ProtectedLoadableResource<E, Any>>

    val dataOptional: Flow<OptionalLoadable<E>>
        get() =
            data.map {
                when (it) {
                    is ProtectedLoadableResource.Failed -> OptionalLoadable.Failed(it.throwable)
                    is ProtectedLoadableResource.Loaded -> OptionalLoadable.LoadedAvailable(it.value)
                    ProtectedLoadableResource.Loading -> OptionalLoadable.Loading
                    is ProtectedLoadableResource.PendingAuthentication<*> -> OptionalLoadable.NotAvailable()
                }
            }

    suspend fun needsUnlock(): Boolean

    suspend fun updateData(transform: suspend (t: E) -> E): E
}
