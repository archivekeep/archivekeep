package org.archivekeep.files.driver.s3

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

class S3RepositoryTestRepo(
    val s3Mock: S3MockContainer,
    val bucketName: String,
) : RepoContractTest.TestRepo<S3Repository> {
    private val credentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create("accessKey", "secretKey"),
        )

    private val s3Client =
        S3Client
            .builder()
            .endpointOverride(URI.create(s3Mock.httpEndpoint))
            .region(Region.of("aa"))
            .credentialsProvider(credentialsProvider)
            .forcePathStyle(true)
            .build()

    override fun open(testDispatcher: TestDispatcher): S3Repository =
        S3Repository(
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
        s3Client.putObject(
            { request ->
                request.bucket(bucketName)
                request.key(key)
            },
            RequestBody.fromString(contents),
        )
    }
}
