package org.archivekeep.app.core.domain.repositories

import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.RepositoryAssociationGroupId

data class ResolvedRepositoryState(
    val uri: RepositoryURI,
    val information: RepositoryInformation,
    val connectionState: Repository.ConnectionState,
) {
    val namedReference: NamedRepositoryReference = NamedRepositoryReference(uri, information.displayName)

    val associationId: RepositoryAssociationGroupId?
        get() = information.associationId

    val displayName: String
        get() = information.displayName
}
