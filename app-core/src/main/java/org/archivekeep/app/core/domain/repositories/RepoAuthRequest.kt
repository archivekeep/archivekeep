package org.archivekeep.app.core.domain.repositories

import org.archivekeep.core.repo.remote.grpc.BasicAuthCredentials

data class RepoAuthRequest(
    val tryOpen: suspend (creds: BasicAuthCredentials, options: UnlockOptions) -> Unit,
)
