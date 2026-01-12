package org.archivekeep.app.core.api.repository.location

import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials

data class UserCredentialsRequest(
    val tryOpen: suspend (creds: BasicAuthCredentials, options: UnlockOptions) -> Unit,
)
