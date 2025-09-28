package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable

abstract class StorageDriver(
    val ID: String,
) {
    abstract fun getStorageAccessor(storageURI: StorageURI): StorageConnection

    abstract fun openLocation(uri: RepositoryURI): RepositoryLocationAccessor
}

open class NeedsUnlock(
    val unlockRequest: Any,
) : OptionalLoadable.NotAvailable(
        RuntimeException("Needs unlock: ${unlockRequest.javaClass.simpleName}"),
    )

data class StorageConnection(
    val storageURI: StorageURI,
    val information: StorageInformation,
    val connectionStatus: SharedFlow<Loadable<Storage.ConnectionStatus>>,
)
