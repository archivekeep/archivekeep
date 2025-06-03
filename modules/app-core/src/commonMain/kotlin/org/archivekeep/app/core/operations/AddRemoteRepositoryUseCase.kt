package org.archivekeep.app.core.operations

import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

interface AddRemoteRepositoryUseCase {
    suspend operator fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
    )
}
