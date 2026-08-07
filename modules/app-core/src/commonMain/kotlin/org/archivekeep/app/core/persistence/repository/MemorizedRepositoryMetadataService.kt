package org.archivekeep.app.core.persistence.repository

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.repositories.RepositoryAccessService
import org.archivekeep.app.core.utils.generics.UniqueInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.stateIn

@Inject
@SingleIn(AppScope::class)
class MemorizedRepositoryMetadataService(
    private val scope: CoroutineScope,
    private val repositoryAccessService: RepositoryAccessService,
    val repository: MemorizedRepositoryMetadataRepository,
) {
    val flows =
        UniqueInstanceManager { uri: RepositoryURI ->
            channelFlow {
                val memorizedMetadataFlow: Flow<OptionalLoadable<RepositoryMetadata>> =
                    repository.repositoryCachedMetadataFlow(uri)

                val updater =
                    launch(start = CoroutineStart.LAZY) {

                        repositoryAccessService
                            .autoUnlockRepositoryAccessor[uri]
                            .filterIsInstance<OptionalLoadable.LoadedAvailable<Repo>>()
                            .collectLatest {
                                it.value.metadataFlow
                                    .conflate()
                                    .collect { metadataLoadable ->
                                        if (metadataLoadable is Loadable.Loaded) {
                                            repository.updateRepositoryMemorizedMetadataIfDiffers(
                                                uri,
                                                metadataLoadable.value,
                                            )
                                        }
                                    }
                            }
                    }

                memorizedMetadataFlow
                    .onStart { updater.start() }
                    .onCompletion { updater.cancel() }
                    .collect { send(it) }
            }.stateIn(scope)
        }
}
