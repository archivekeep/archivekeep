package org.archivekeep.files.driver.s3

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.archivekeep.files.shouldHaveCommittedContentsOf
import org.archivekeep.files.testContents01
import org.archivekeep.files.withContentsFrom
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class S3RepositoryTest {
    private val bucketName = "test-bucket"

    @Container
    private val s3Mock =
        S3MockContainer(DockerImageName.parse("adobe/s3mock:4.3.0"))
            .withEnv("initialBuckets", bucketName)

    @Test
    fun `contents should not be affected by objects outside files directory`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val testRepo = S3RepositoryTestRepo(s3Mock, "test-bucket")

            val accessor =
                testRepo
                    .open(dispatcher)
                    .withContentsFrom(testContents01)

            testRepo.createObject("something-else.txt")
            testRepo.createObject("other-thing.txt")

            accessor shouldHaveCommittedContentsOf testContents01
        }
}
