package org.archivekeep.app.core.persistence.platform.demo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.utils.generics.UniqueInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.utils.loading.optional.OptionalLoadable

class InMemoryMemorizedRepositoryMetadataRepository : MemorizedRepositoryMetadataRepository {
    private val repositoryMetadataFlowManager =
        UniqueInstanceManager<RepositoryURI, MutableStateFlow<OptionalLoadable<RepositoryMetadata>>> {
            MutableStateFlow(OptionalLoadable.NotAvailable())
        }

    override fun repositoryCachedMetadataFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepositoryMetadata>> = repositoryMetadataFlowManager[uri]

    override suspend fun updateRepositoryMemorizedMetadataIfDiffers(
        uri: RepositoryURI,
        metadata: RepositoryMetadata,
    ) {
        repositoryMetadataFlowManager[uri].update {
            OptionalLoadable.LoadedAvailable(metadata)
        }
    }
}
