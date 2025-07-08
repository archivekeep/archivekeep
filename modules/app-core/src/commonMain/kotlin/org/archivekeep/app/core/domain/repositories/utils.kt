package org.archivekeep.app.core.domain.repositories

import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.utils.exceptions.DisconnectedStorageException
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.ProtectedLoadableResource

fun (ProtectedLoadableResource<Repo, RepoAuthRequest>).asOptionalLoadable() =
    when (this) {
        is ProtectedLoadableResource.Failed -> {
            if (this.throwable is DisconnectedStorageException) {
                OptionalLoadable.NotAvailable(this.throwable)
            } else {
                OptionalLoadable.Failed(this.throwable)
            }
        }

        is ProtectedLoadableResource.Loaded -> OptionalLoadable.LoadedAvailable(this.value)
        ProtectedLoadableResource.Loading -> OptionalLoadable.Loading
        is ProtectedLoadableResource.PendingAuthentication -> NeedsUnlock(authenticationRequest)
    }
