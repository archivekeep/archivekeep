package org.archivekeep.files.repo.auth

import kotlinx.serialization.Serializable

@Serializable
data class BasicAuthCredentials(
    val username: String,
    val password: String,
)
