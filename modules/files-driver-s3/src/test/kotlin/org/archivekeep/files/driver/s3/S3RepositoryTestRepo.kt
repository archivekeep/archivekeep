package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.utils.fromHexToBase64
import org.archivekeep.utils.sha256
import java.net.URI

class S3RepositoryTestRepo(
    val s3Mock: S3MockContainer,
    val bucketName: String,
) : RepoContractTest.TestRepo<S3Repository> {
    private val credentialsProvider =
        StaticCredentialsProvider {
            accessKeyId = "accessKey"
            secretAccessKey = "secretAccessKey"
        }

    override suspend fun open(testDispatcher: TestDispatcher): S3Repository =
        openS3Repository(
            URI.create(s3Mock.httpEndpoint),
            "aa",
            credentialsProvider,
            bucketName,
            sharingDispatcher = testDispatcher,
            ioDispatcher = testDispatcher,
        )

    suspend fun createObject(
        key: String,
        contents: String = key,
    ) {
        S3Client
            .fromEnvironment {
                endpointUrl = Url.parse(s3Mock.httpEndpoint)
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
