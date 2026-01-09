package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import kotlinx.coroutines.CoroutineDispatcher
import org.archivekeep.files.repo.RepoContractTest
import java.net.URI
import java.util.UUID

class EncryptedS3RepositoryTestRepo private constructor(
    val s3URL: String,
    val bucketName: String,
    val accessKey: String,
    val secretKey: String,
    val password: String,
) : RepoContractTest.TestRepo<EncryptedS3Repository> {
    private val credentialsProvider =
        StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }

    companion object {
        suspend fun createIn(
            s3URL: String,
            bucketName: String,
            accessKey: String = "NONE",
            secretKey: String = "NONE",
        ): EncryptedS3RepositoryTestRepo {
            val password = UUID.randomUUID().toString()

            return EncryptedS3RepositoryTestRepo(
                s3URL,
                bucketName,
                accessKey,
                secretKey,
                password,
            ).apply {
                EncryptedS3Repository.create(
                    URI.create(s3URL),
                    "aa",
                    credentialsProvider,
                    bucketName,
                    password,
                )
            }
        }
    }

    override suspend fun open(testDispatcher: CoroutineDispatcher): EncryptedS3Repository =
        EncryptedS3Repository
            .openAndUnlock(
                URI.create(s3URL),
                "aa",
                credentialsProvider,
                bucketName,
                password,
                sharingDispatcher = testDispatcher,
            ).apply {
                vault.unlock(password)
            }
}
