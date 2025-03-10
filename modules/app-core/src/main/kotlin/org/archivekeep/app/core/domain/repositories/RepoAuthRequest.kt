package org.archivekeep.app.core.domain.repositories

import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

data class RepoAuthRequest(
    val tryOpen: suspend (creds: BasicAuthCredentials, options: UnlockOptions) -> Unit,
)
