package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import java.net.URI

class EncryptedS3RepositoryTestRepo(
    val s3URL: String,
    val bucketName: String,
    val accessKey: String = "NONE",
    val secretKey: String = "NONE",
) : RepoContractTest.TestRepo<EncryptedS3Repository> {
    private val credentialsProvider =
        StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }

    override suspend fun open(testDispatcher: TestDispatcher): EncryptedS3Repository =
        EncryptedS3Repository.open(
            URI.create(s3URL),
            "aa",
            credentialsProvider,
            bucketName,
            sharingDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
        )
}
