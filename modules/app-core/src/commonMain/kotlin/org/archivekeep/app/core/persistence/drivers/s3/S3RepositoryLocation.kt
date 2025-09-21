package org.archivekeep.app.core.persistence.drivers.s3

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationDiscoveryForAdd
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationDiscoveryOutcome
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3LocationNotInitializedAsRepositoryException
import org.archivekeep.files.driver.s3.S3Repository
import java.net.URI

sealed interface S3RepositoryLocation : RepositoryLocationDiscoveryForAdd {
    class ContainingS3Repository(
        val connection: S3Repository,
        val onPreserveCredentials: suspend (rememberCredentials: Boolean) -> Unit,
    ) : S3RepositoryLocation {
        override fun asRepositoryLocationDiscoveryOutcome() = RepositoryLocationDiscoveryOutcome.IsRepositoryLocation

        override suspend fun preserveCredentialss(rememberCredentials: Boolean) {
            onPreserveCredentials(rememberCredentials)
        }
    }

    class ContainingEncryptedS3Repository(
        val connection: EncryptedS3Repository,
        val onPreserveCredentials: suspend (rememberCredentials: Boolean) -> Unit,
    ) : S3RepositoryLocation {
        override fun asRepositoryLocationDiscoveryOutcome() = RepositoryLocationDiscoveryOutcome.IsRepositoryLocation

        override suspend fun preserveCredentialss(rememberCredentials: Boolean) {
            onPreserveCredentials(rememberCredentials)
        }
    }

    class LocationCanBeInitialized(
        val initializeAsPlain: (suspend () -> Unit)?,
        val initializeAsE2EEPasswordProtected: (suspend (password: String) -> Unit)?,
        val onPreserveCredentials: suspend (rememberCredentials: Boolean) -> Unit,
    ) : S3RepositoryLocation {
        override fun asRepositoryLocationDiscoveryOutcome() =
            RepositoryLocationDiscoveryOutcome.LocationCanBeInitialized(
                initializeAsPlain,
                initializeAsE2EEPasswordProtected,
            )

        override suspend fun preserveCredentialss(rememberCredentials: Boolean) {
            onPreserveCredentials(rememberCredentials)
        }
    }

    companion object {
        suspend fun open(
            endpoint: URI,
            region: String,
            credentialsProvider: CredentialsProvider,
            bucketName: String,
            onPreserveCredentials: suspend (rememberCredentials: Boolean) -> Unit = {},
        ): S3RepositoryLocation {
            try {
                val connection =
                    EncryptedS3Repository.open(endpoint, region, credentialsProvider, bucketName)

                return ContainingEncryptedS3Repository(connection, onPreserveCredentials)
            } catch (_: S3LocationNotInitializedAsRepositoryException) {
                // ignore, it's one of happy paths for discovery
            }

            try {
                val connection =
                    S3Repository.open(endpoint, region, credentialsProvider, bucketName)

                return ContainingS3Repository(connection, onPreserveCredentials)
            } catch (_: S3LocationNotInitializedAsRepositoryException) {
                // ignore, it's one of happy paths for discovery
            }

            return LocationCanBeInitialized(
                initializeAsPlain = {
                    S3Repository.create(endpoint, region, credentialsProvider, bucketName)
                },
                initializeAsE2EEPasswordProtected = { password ->
                    EncryptedS3Repository.create(endpoint, region, credentialsProvider, bucketName, password)
                },
                onPreserveCredentials,
            )
        }
    }
}
