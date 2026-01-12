package org.archivekeep.files.api.repository.auth

import kotlinx.serialization.Serializable

@Serializable
data class BasicAuthCredentials(
    val username: String,
    val password: String,
)
