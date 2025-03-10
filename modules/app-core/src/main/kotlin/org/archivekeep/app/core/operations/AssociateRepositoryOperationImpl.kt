package org.archivekeep.app.core.operations

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.operations.AssociateRepositoryOperation.Target
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import java.util.UUID

class AssociateRepositoryOperationImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
    val uri: RepositoryURI,
) : AssociateRepositoryOperation {
    val repository = repositoryService.getRepository(uri)

    override suspend fun execute(target: Target): ExecutionOutcome {
        val selectedItem = target

        try {
            if (selectedItem is Target.Archive) {
                repository.updateMetadata {
                    it.copy(
                        associationGroupId = selectedItem.associatedArchiveId,
                    )
                }
            } else if (selectedItem is Target.UnassociatedRepository) {
                val newUUID = UUID.randomUUID().toString()

                val otherURI =
                    selectedItem.repositoryURI

                println(
                    "Update association ID of $uri and $otherURI to new $newUUID",
                )

                val otherItemRepo =
                    repositoryService
                        .getRepository(
                            otherURI,
                        )

                repository.updateMetadata {
                    println("Updating $uri")
                    it.copy(
                        associationGroupId = newUUID,
                    )
                }
                otherItemRepo.updateMetadata {
                    println("Updating $otherURI")
                    it.copy(
                        associationGroupId = newUUID,
                    )
                }
            }

            return ExecutionOutcome.Success()
        } catch (e: Exception) {
            return ExecutionOutcome.Failed(e)
        }
    }
}
