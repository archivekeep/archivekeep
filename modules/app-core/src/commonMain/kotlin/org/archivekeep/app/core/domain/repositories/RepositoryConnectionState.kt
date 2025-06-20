package org.archivekeep.app.core.domain.repositories

sealed class RepositoryConnectionState(
    val isAvailable: Boolean,
    val isConnected: Boolean,
    val isLocked: Boolean,
) {
    data object Disconnected : RepositoryConnectionState(false, false, false)

    data object ConnectedLocked : RepositoryConnectionState(true, false, true)

    data object Connected : RepositoryConnectionState(true, true, false)

    class Error(
        cause: Throwable?,
    ) : RepositoryConnectionState(false, false, false)
}
