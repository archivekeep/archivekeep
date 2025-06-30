package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.ProtectedLoadableResource

abstract class StorageDriver(
    val ID: String,
) {
    abstract fun getStorageAccessor(storageURI: StorageURI): StorageConnection

    abstract fun openRepoFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>>
}

data class StorageConnection(
    val storageURI: StorageURI,
    val information: StorageInformation,
    val connectionStatus: SharedFlow<Loadable<Storage.ConnectionStatus>>,
)
