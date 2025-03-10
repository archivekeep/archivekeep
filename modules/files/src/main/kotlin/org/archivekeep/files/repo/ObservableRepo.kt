package org.archivekeep.files.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.utils.loading.Loadable

interface ObservableRepo {
    val indexFlow: SharedFlow<Loadable<RepoIndex>>
    val metadataFlow: Flow<Loadable<RepositoryMetadata>>
}
