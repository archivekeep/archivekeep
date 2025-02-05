package org.archivekeep.app.core.domain.repositories

import org.archivekeep.core.RepositoryAssociationGroupId

data class RepositoryInformation(
    val associationId: RepositoryAssociationGroupId?,
    val displayName: String,
)
