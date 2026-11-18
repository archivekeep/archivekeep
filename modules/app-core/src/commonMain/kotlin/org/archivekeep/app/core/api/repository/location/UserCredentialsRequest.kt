package org.archivekeep.app.core.api.repository.location

import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials

enum class UserCredentialsInputType {
    BASIC_AUTH,
    GOOGLE_DRIVE_CREDENTIALS_JSON,
}

data class UserCredentialsRequest(
    val tryOpen: suspend (creds: BasicAuthCredentials, options: UnlockOptions) -> Unit,
    val inputType: UserCredentialsInputType = UserCredentialsInputType.BASIC_AUTH,
)
