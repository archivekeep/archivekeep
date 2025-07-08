package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.loading.Loadable

abstract class StorageDriver(
    val ID: String,
) {
    abstract fun getStorageAccessor(storageURI: StorageURI): StorageConnection

    abstract fun getProvider(uri: RepositoryURI): RepositoryAccessorProvider
}

interface RepositoryAccessorProvider {
    /**
     * The current state of available accessor to repository contents.
     *
     * Should use [NeedsUnlock] to signal [OptionalLoadable.NotAvailable] due to pending authentication request.
     */
    val repositoryAccessor: Flow<RepositoryAccessState>
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
