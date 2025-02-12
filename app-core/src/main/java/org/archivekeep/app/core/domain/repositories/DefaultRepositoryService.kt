package org.archivekeep.app.core.domain.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.generics.UniqueInstanceManager
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.Repo

class DefaultRepositoryService(
    private val scope: CoroutineScope,
    private val storageDrivers: Map<String, StorageDriver>,
    private val registry: RegistryDataStore,
    private val memorizedRepositoryIndexRepository: MemorizedRepositoryIndexRepository,
    private val memorizedRepositoryMetadataRepository: MemorizedRepositoryMetadataRepository,
) : RepositoryService {
    private val repositoryStates =
        UniqueInstanceManager(factory = { uri: RepositoryURI ->
            Repository(
                scope,
                uri = uri,
                registeredRepositoryFlow =
                    registry.registeredRepositories.map { registeredRepositories ->
                        registeredRepositories.firstOrNull { it.uri == uri }
                    },
                rawAccessor = archiveInstance[uri],
                memorizedRepositoryIndexRepository = memorizedRepositoryIndexRepository,
                memorizedRepositoryMetadataRepository = memorizedRepositoryMetadataRepository,
            )
        })
    private val archiveInstance = UniqueSharedFlowInstanceManager(scope, factory = ::createBase)

    override fun getRepository(repositoryURI: RepositoryURI) = repositoryStates[repositoryURI]

    override suspend fun registerRepository(repositoryURI: RepositoryURI) {
        registry.updateRepositories { old ->
            old + setOf(RegisteredRepository(uri = repositoryURI))
        }
    }

    private fun createBase(repositoryURI: RepositoryURI): Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>> {
        println("GET REPO FOR: $repositoryURI")

        val driver =
            storageDrivers[repositoryURI.driver]
                ?: return flowOf(ProtectedLoadableResource.Failed(RuntimeException("Driver ${repositoryURI.driver} not supported")))

        return driver.openRepoFlow(repositoryURI)
    }
}
