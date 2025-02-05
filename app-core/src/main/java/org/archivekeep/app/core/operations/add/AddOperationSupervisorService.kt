package org.archivekeep.app.core.operations.add

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface AddOperationSupervisorService {
    fun getAddOperation(repositoryURI: RepositoryURI): AddOperationSupervisor
}
