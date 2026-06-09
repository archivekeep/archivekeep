package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.utils.identifiers.StorageURI

data class DemoOnlineStorage(
    val displayName: String,
    val connectionStatus: Storage.ConnectionStatus,
    val id: String = displayName.toSlug(),
    val repositories: List<DemoRepository>,
) {
    val uri: StorageURI
        get() = StorageURI(DemoRepositoryURIData.ID, id)

    val reference = StorageNamedReference(uri, displayName)
}
