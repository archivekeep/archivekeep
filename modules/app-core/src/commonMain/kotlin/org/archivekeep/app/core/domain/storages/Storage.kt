package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.combineToList
import org.archivekeep.utils.coroutines.InstanceProtector
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLatestLoadedData
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.stateIn

private val InstanceProtector = InstanceProtector<Storage>()

class Storage(
    baseScope: CoroutineScope,
    private val repositoryService: RepositoryService,
    val uri: StorageURI,
    val knownStorageFlow: Flow<Loadable<KnownStorage>>,
    val connection: StorageConnection,
) {
    private val scope = baseScope + InstanceProtector.forInstance(this)

    data class State(
        val connectionStatus: ConnectionStatus,
    ) {
        val isConnected = connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.ONLINE
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val repositories: SharedFlow<Loadable<List<StorageRepository>>> =
        knownStorageFlow
            .flatMapLatestLoadedData { knownStorage ->
                val storageRef = StorageNamedReference(uri, knownStorage.label)

                combineToList(
                    knownStorage.registeredRepositories.map { registeredRepo ->
                        repositoryService
                            .getRepository(registeredRepo.uri)
                            .resolvedState
                            .map {
                                StorageRepository(
                                    storageRef,
                                    it.uri,
                                    it,
                                )
                            }
                    },
                )
            }.stateIn(scope)

    enum class ConnectionStatus {
        ONLINE,
        CONNECTED,
        DISCONNECTED,
    }

    val state =
        connection.connectionStatus
            .mapLoadedData(::State)
            .stateIn(scope, SharingStarted.WhileSubscribed(), Loadable.Loading)

    val partiallyResolved =
        knownStorageFlow
            .mapLoadedData { knownStorage ->
                StoragePartiallyResolved(
                    scope,
                    uri,
                    knownStorage,
                    this,
                )
            }.stateIn(scope)
}
