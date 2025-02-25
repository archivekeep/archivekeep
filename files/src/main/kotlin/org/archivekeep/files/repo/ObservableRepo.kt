package org.archivekeep.files.repo

import kotlinx.coroutines.flow.Flow
import org.archivekeep.utils.loading.Loadable

interface ObservableRepo {
    val indexFlow: Flow<Loadable<RepoIndex>>
    val metadataFlow: Flow<Loadable<RepositoryMetadata>>
}
