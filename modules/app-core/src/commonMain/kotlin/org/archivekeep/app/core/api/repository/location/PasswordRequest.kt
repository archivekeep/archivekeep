package org.archivekeep.app.core.api.repository.location

data class PasswordRequest(
    val providePassword: suspend (password: String, rememberPassword: Boolean) -> Unit,
)
