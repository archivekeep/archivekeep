package org.archivekeep.files.repo.remote.grpc

import kotlinx.serialization.Serializable

@Serializable
data class BasicAuthCredentials(
    val username: String,
    val password: String,
)
