package org.archivekeep.files.driver.s3

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import java.net.URI

@Testcontainers
class S3RepositoryTest : RepoContractTest<S3Repository>() {
    private val bucketName = "test-bucket"

    @Container
    private val s3Mock =
        S3MockContainer(DockerImageName.parse("adobe/s3mock:4.3.0"))
            .withEnv("initialBuckets", bucketName)

    override fun createNew(): TestRepo<S3Repository> =
        object : TestRepo<S3Repository> {
            override fun open(testDispatcher: TestDispatcher): S3Repository =
                S3Repository(
                    URI.create(s3Mock.httpEndpoint),
                    "aa",
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("accessKey", "secretKey"),
                    ),
                    bucketName,
                    sharingDispatcher = testDispatcher,
                    ioDispatcher = testDispatcher,
                )
        }
}
