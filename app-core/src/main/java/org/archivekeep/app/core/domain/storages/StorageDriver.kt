package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.core.repo.Repo

interface StorageDriver {
    fun getStorageAccessor(storageURI: StorageURI): StorageConnection

    fun openRepoFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>>
}

data class StorageConnection(
    val storageURI: StorageURI,
    val information: Flow<OptionalLoadable<StorageInformation>>,
    val connectionStatus: SharedFlow<Storage.ConnectionStatus>,
)
