package org.archivekeep.app.core.utils.identifiers

data class NamedRepositoryReference(
    val uri: RepositoryURI,
    val displayName: String,
)
