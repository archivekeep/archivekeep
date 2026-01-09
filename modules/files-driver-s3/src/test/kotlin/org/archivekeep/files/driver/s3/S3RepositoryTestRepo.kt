package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.createBucket
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.CoroutineDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.utils.fromHexToBase64
import org.archivekeep.utils.hashing.sha256
import java.net.URI

class S3RepositoryTestRepo(
    val s3URL: String,
    val bucketName: String,
    val accessKey: String = "NONE",
    val secretKey: String = "NONE",
) : RepoContractTest.TestRepo<S3Repository> {
    private val credentialsProvider =
        StaticCredentialsProvider {
            accessKeyId = accessKey
            secretAccessKey = secretKey
        }

    suspend fun create(): S3Repository =
        S3Repository.create(
            URI.create(s3URL),
            "aa",
            credentialsProvider,
            bucketName,
        )

    override suspend fun open(testDispatcher: CoroutineDispatcher): S3Repository =
        S3Repository.open(
            URI.create(s3URL),
            "aa",
            credentialsProvider,
            bucketName,
            sharingDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
        )

    suspend fun createBucket() {
        S3Client
            .fromEnvironment {
                endpointUrl = Url.parse(s3URL)
                region = "TODO"
                credentialsProvider = this@S3RepositoryTestRepo.credentialsProvider
                forcePathStyle = true
            }.use { s3 ->
                try {
                    s3.createBucket {
                        bucket = bucketName
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
    }

    suspend fun createObject(
        key: String,
        contents: String = key,
    ) {
        S3Client
            .fromEnvironment {
                endpointUrl = Url.parse(s3URL)
                region = "TODO"
                credentialsProvider = this@S3RepositoryTestRepo.credentialsProvider
                forcePathStyle = true
            }.use { s3 ->
                try {
                    s3.putObject {
                        val bytes = contents.toByteArray()

                        bucket = bucketName
                        this.key = key
                        checksumSha256 = bytes.sha256().fromHexToBase64()

                        body = bytes.inputStream().asByteStream(bytes.size.toLong())
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
    }
}
