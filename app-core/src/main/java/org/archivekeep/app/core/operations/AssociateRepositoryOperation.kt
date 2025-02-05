package org.archivekeep.app.core.operations

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.core.RepositoryAssociationGroupId

interface AssociateRepositoryOperation {
    interface Factory {
        fun create(
            scope: CoroutineScope,
            uri: RepositoryURI,
        ): AssociateRepositoryOperation
    }

    sealed interface Target {
        data class Archive(
            val associatedArchiveId: RepositoryAssociationGroupId,
        ) : Target

        data class UnassociatedRepository(
            val repositoryURI: RepositoryURI,
        ) : Target
    }

    suspend fun execute(target: Target): ExecutionOutcome
}
