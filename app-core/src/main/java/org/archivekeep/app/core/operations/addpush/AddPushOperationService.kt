package org.archivekeep.app.core.operations.addpush

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface AddPushOperationService {
    fun getAddPushOperation(repositoryURI: RepositoryURI): RepoAddPush
}
