package org.archivekeep.app.core.domain.repositories

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

@Inject
@SingleIn(AppScope::class)
class RepositoryRegistryService(
    val registry: RegistryDataStore,
    val repositoryAccessService: RepositoryAccessService,
) {
    suspend fun forgetRepository(uri: RepositoryURI) {
        registry.updateRepositories { old ->
            old.filter { it.uri != uri }.toSet()
        }

        repositoryAccessService.autoUnlockRepositoryAccessor.invalidate(uri)
        repositoryAccessService.repositoryAccessor.invalidate(uri)
    }
}
