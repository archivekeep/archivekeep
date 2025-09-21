package org.archivekeep.app.core.operations

import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

sealed interface AddRemoteRepositoryOutcome {
    data object Added : AddRemoteRepositoryOutcome

    class NeedsInitialization(
        val initializeAsPlain: (suspend () -> Unit)?,
        val initializeAsE2EEPasswordProtected: (suspend (password: String) -> Unit)?,
    ) : AddRemoteRepositoryOutcome
}

interface AddRemoteRepositoryUseCase {
    suspend operator fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
        rememberCredentials: Boolean,
    ): AddRemoteRepositoryOutcome
}

suspend fun AddRemoteRepositoryUseCase.addS3(
    endpoint: String,
    bucket: String,
    accessKey: String,
    secretKey: String,
    rememberCredentials: Boolean,
): AddRemoteRepositoryOutcome =
    this(
        S3RepositoryURIData(endpoint, bucket).toURI(),
        BasicAuthCredentials(accessKey, secretKey),
        rememberCredentials,
    )
