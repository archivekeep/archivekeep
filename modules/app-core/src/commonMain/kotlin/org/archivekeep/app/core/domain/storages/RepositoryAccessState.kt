package org.archivekeep.app.core.domain.storages

import org.archivekeep.app.core.domain.repositories.RepositoryConnectionState
import org.archivekeep.app.core.utils.exceptions.DisconnectedStorageException
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable

typealias RepositoryAccessState = OptionalLoadable<Repo>

fun RepositoryAccessState.asStatus(): RepositoryConnectionState =
    when (this) {
        is OptionalLoadable.Failed -> {
            if (this.cause is DisconnectedStorageException) {
                RepositoryConnectionState.Disconnected
            } else {
                RepositoryConnectionState.Error(this.cause)
            }
        }

        is OptionalLoadable.LoadedAvailable -> RepositoryConnectionState.Connected
        OptionalLoadable.Loading -> RepositoryConnectionState.Disconnected
        // TODO: better
        is OptionalLoadable.NotAvailable -> RepositoryConnectionState.ConnectedLocked
    }

fun RepositoryAccessState.asUnlockRequest() = (this as? NeedsUnlock)?.unlockRequest

fun RepositoryAccessState.asLoadableUnlockRequest(): Loadable<Any?> =
    when (this) {
        is OptionalLoadable.Failed -> Loadable.Failed(this.cause)
        OptionalLoadable.Loading -> Loadable.Loading
        is OptionalLoadable.LoadedAvailable<*> -> Loadable.Loaded(null)
        is NeedsUnlock -> Loadable.Loaded(this.unlockRequest)
        is OptionalLoadable.NotAvailable -> Loadable.Loaded(null)
    }

fun RepositoryAccessState.needsUnlock() = this is NeedsUnlock
