package org.archivekeep.app.core.domain.repositories

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemRepositoryURIData
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

@Inject
@SingleIn(AppScope::class)
class RepositoryRegistryService(
    val registry: RegistryDataStore,
    val repositoryAccessService: RepositoryAccessService,
    val memorizedRepositoryIndexRepository: MemorizedRepositoryIndexRepository,
    val memorizedRepositoryMetadataRepository: MemorizedRepositoryMetadataRepository,
    val memorizedRepositoryMetadataService: MemorizedRepositoryMetadataService,
    val repositoryService: RepositoryService,
    val storageDrivers: Map<String, StorageDriver>,
) {
    suspend fun forgetRepository(uri: RepositoryURI) {
        registry.updateRepositories { old ->
            old.filter { it.uri != uri }.toSet()
        }

        repositoryAccessService.autoUnlockRepositoryAccessor.invalidate(uri)
        repositoryAccessService.repositoryAccessor.invalidate(uri)
        memorizedRepositoryIndexRepository.removeRepositoryMemorizedIndex(uri)
        memorizedRepositoryMetadataRepository.removeRepositoryMemorizedMetadata(uri)
        memorizedRepositoryMetadataService.flows.invalidate(uri)

        repositoryService.invalidateRepository(uri)

        val typedData = uri.typedRepoURIData as? FileSystemRepositoryURIData
        if (typedData != null) {
            (storageDrivers[FileSystemRepositoryURIData.ID] as? FileSystemStorageDriver)
                ?.providers
                ?.invalidate(Pair(uri, typedData))
        }
    }
}
