package org.archivekeep.app.core.persistence.credentials

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

interface CredentialsStore {
    fun getRepositoryCredentialsFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<BasicAuthCredentials?, Any>>

    suspend fun rememberRepositoryCredentials(
        repositoryURI: RepositoryURI,
        credentials: BasicAuthCredentials,
    )
}
