package org.archivekeep.files.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.utils.loading.Loadable

interface ObservableRepo {
    val indexFlow: StateFlow<Loadable<RepoIndex>>
    val metadataFlow: Flow<Loadable<RepositoryMetadata>>
}
