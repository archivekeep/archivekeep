package org.archivekeep.app.core.procedures.sync

import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface RepoToRepoSyncService {
    fun getRepoToRepoSync(
        baseURI: RepositoryURI,
        otherURI: RepositoryURI,
    ): RepoToRepoSync
}
