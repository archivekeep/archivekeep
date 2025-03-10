package org.archivekeep.app.core.domain.repositories

import org.archivekeep.files.RepositoryAssociationGroupId

data class RepositoryInformation(
    val associationId: RepositoryAssociationGroupId?,
    val displayName: String,
)
