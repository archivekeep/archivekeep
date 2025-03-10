package org.archivekeep.app.core.domain.storages

import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.utils.identifiers.StorageURI

data class KnownStorage(
    val storageURI: StorageURI,
    val registeredStorage: RegisteredStorage?,
    val registeredRepositories: List<RegisteredRepository>,
) {
    val label: String
        get() = registeredStorage?.label ?: storageURI.substituteLabel()

    val isLocal: Boolean
        get() = registeredStorage?.isLocal ?: false
}
