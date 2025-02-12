package org.archivekeep.app.core.persistence.registry

import kotlinx.serialization.Serializable
import org.archivekeep.app.core.utils.identifiers.StorageURI

@Serializable
data class RegisteredStorage(
    val uri: StorageURI,
    val label: String? = null,
    val isLocal: Boolean? = null,
)
