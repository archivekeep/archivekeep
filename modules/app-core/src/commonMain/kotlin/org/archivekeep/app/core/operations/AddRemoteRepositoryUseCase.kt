package org.archivekeep.app.core.operations

import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

interface AddRemoteRepositoryUseCase {
    suspend operator fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
        rememberCredentials: Boolean,
    )
}

suspend fun AddRemoteRepositoryUseCase.addS3(
    endpoint: String,
    bucket: String,
    accessKey: String,
    secretKey: String,
    rememberCredentials: Boolean,
) {
    this(
        S3RepositoryURIData(endpoint, bucket).toURI(),
        BasicAuthCredentials(accessKey, secretKey),
        rememberCredentials,
    )
}
