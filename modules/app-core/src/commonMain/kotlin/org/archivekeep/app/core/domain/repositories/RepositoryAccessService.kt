package org.archivekeep.app.core.domain.repositories

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.api.repository.location.autoUnlocker
import org.archivekeep.app.core.api.repository.location.repositoryAccessor
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.getDriverForURI
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.generics.UniqueInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.utils.loading.optional.OptionalLoadable.Failed
import org.archivekeep.utils.loading.optional.stateIn

@Inject
@SingleIn(AppScope::class)
class RepositoryAccessService(
    private val scope: CoroutineScope,
    private val storageDrivers: Map<String, StorageDriver>,
    private val credentialsStore: CredentialsStore,
) {
    val repositoryAccessor = UniqueInstanceManager(factory = ::createBase)

    val autoUnlockRepositoryAccessor =
        UniqueInstanceManager { uri: RepositoryURI ->
            repositoryAccessor[uri]
                .contentsStateFlow
                .repositoryAccessor()
                .autoUnlocker(uri, credentialsStore)
                .stateIn(scope)
        }

    private fun createBase(repositoryURI: RepositoryURI): RepositoryLocationAccessor {
        val driver =
            storageDrivers.getDriverForURI(repositoryURI)
                ?: return object : RepositoryLocationAccessor {
                    override val contentsStateFlow =
                        flowOf(
                            Failed(RuntimeException("Driver ${repositoryURI.driver} not supported")),
                        )
                }

        return driver.openLocation(repositoryURI)
    }
}
