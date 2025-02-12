package org.archivekeep.app.core.operations.derived

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface SyncService {
    fun getRepoToRepoSync(
        baseURI: RepositoryURI,
        otherURI: RepositoryURI,
    ): RepoToRepoSync
}
