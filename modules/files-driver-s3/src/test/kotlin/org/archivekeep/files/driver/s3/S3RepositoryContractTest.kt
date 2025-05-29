package org.archivekeep.files.driver.s3

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import org.archivekeep.files.repo.RepoContractTest
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class S3RepositoryContractTest : RepoContractTest<S3Repository>() {
    private val bucketName = "test-bucket"

    @Container
    private val s3Mock =
        S3MockContainer(DockerImageName.parse("adobe/s3mock:4.3.0"))
            .withEnv("initialBuckets", bucketName)

    override fun createNew(): TestRepo<S3Repository> = S3RepositoryTestRepo(s3Mock, bucketName)
}
