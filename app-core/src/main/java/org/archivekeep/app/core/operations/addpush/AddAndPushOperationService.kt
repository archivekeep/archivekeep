package org.archivekeep.app.core.operations.addpush

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface AddAndPushOperationService {
    fun getAddPushOperation(repositoryURI: RepositoryURI): AddAndPushOperation
}
