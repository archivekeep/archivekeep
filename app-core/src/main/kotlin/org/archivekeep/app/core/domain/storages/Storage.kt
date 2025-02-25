package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable

class Storage(
    val uri: StorageURI,
    val knownStorage: KnownStorage,
    val connection: StorageConnection,
    val repositories: SharedFlow<List<StorageRepository>>,
) {
    data class State(
        val connectionStatus: ConnectionStatus,
    ) {
        val isConnected = connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.ONLINE
    }

    val isLocal: Boolean
        get() = knownStorage.isLocal

    val label: String
        get() = knownStorage.label

    val namedReference =
        StorageNamedReference(
            uri,
            label,
        )

    enum class ConnectionStatus {
        ONLINE,
        CONNECTED,
        DISCONNECTED,
    }

    val state =
        connection.connectionStatus
            .mapToLoadable { connectionStatus ->
                State(connectionStatus)
            }.stateIn(GlobalScope, SharingStarted.WhileSubscribed(), Loadable.Loading)
}

data class StorageNamedReference(
    val uri: StorageURI,
    val displayName: String,
)
