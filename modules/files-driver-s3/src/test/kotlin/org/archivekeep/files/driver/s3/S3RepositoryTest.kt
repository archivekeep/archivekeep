package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.services.s3.model.NoSuchBucket
import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.archivekeep.files.shouldHaveCommittedContentsOf
import org.archivekeep.files.testContents01
import org.archivekeep.files.withContentsFrom
import org.archivekeep.utils.exceptions.WrongCredentialsException
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class S3RepositoryTest {
    private val bucketName = "test-bucket"
    private val accessKey = "foo"
    private val secretKey = "bar"

    @Container
    var s3Mock: S3MockContainer =
        S3MockContainer(DockerImageName.parse("adobe/s3mock:4.3.0"))

    @Test
    fun `contents should not be affected by objects outside files directory`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey)
            testRepo.createBucket()
            testRepo.create()

            val accessor =
                testRepo
                    .open(dispatcher)
                    .withContentsFrom(testContents01)

            testRepo.createObject("something-else.txt")
            testRepo.createObject("other-thing.txt")

            accessor shouldHaveCommittedContentsOf testContents01
        }

    @Test
    fun `should fail on missing bucket`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, "missing-bucket", accessKey, secretKey)

            assertThrows<NoSuchBucket> {
                testRepo.open(dispatcher)
            }
        }

    // TODO: either fix test or alter behaviour of implementation
    @Disabled("Difference in behaviour of S3 test/mock implementation")
    @Test
    fun `should fail on wrong access key`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey + "corruption", secretKey)

            assertThrows<WrongCredentialsException> {
                testRepo.open(dispatcher)
            }
        }

    // TODO: either fix test or alter behaviour of implementation
    @Disabled("Difference in behaviour of S3 test/mock implementation")
    @Test
    fun `should fail on wrong secret key`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey + "corruption")

            assertThrows<WrongCredentialsException> {
                testRepo.open(dispatcher)
            }
        }
}
