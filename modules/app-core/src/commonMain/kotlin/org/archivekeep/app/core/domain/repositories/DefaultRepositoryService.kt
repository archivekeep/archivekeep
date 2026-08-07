package org.archivekeep.app.core.domain.repositories

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataService
import org.archivekeep.app.core.utils.generics.UniqueInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(CoreApplicationServiceScope::class)
class DefaultRepositoryService(
    private val scope: CoroutineScope,
    private val repositoryAccessService: RepositoryAccessService,
    private val memorizedRepositoryMetadataService: MemorizedRepositoryMetadataService,
    private val registry: RegistryDataStore,
    private val memorizedRepositoryIndexRepository: MemorizedRepositoryIndexRepository,
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
                optionalAccessorFlow = repositoryAccessService.autoUnlockRepositoryAccessor[uri],
                memorizedRepositoryIndexRepository = memorizedRepositoryIndexRepository,
                memorizedRepositoryMetadataService = memorizedRepositoryMetadataService,
            )
        })

    override fun getRepository(repositoryURI: RepositoryURI) = repositoryStates[repositoryURI]

    override suspend fun registerRepository(repositoryURI: RepositoryURI) {
        registry.updateRepositories { old ->
            old + setOf(RegisteredRepository(uri = repositoryURI))
        }
    }
}
