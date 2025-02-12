package org.archivekeep.files.repo

import kotlinx.coroutines.flow.Flow
import org.archivekeep.utils.Loadable

interface ObservableRepo {
    val indexFlow: Flow<RepoIndex>
    val metadataFlow: Flow<Loadable<RepositoryMetadata>>
}
