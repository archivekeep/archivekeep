package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.services.s3.model.NoSuchBucket
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.archivekeep.files.shouldHaveCommittedContentsOf
import org.archivekeep.files.testContents01
import org.archivekeep.files.withContentsFrom
import org.archivekeep.utils.exceptions.WrongCredentialsException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class S3RepositoryTest {
    @Container
    var minio: MinIOContainer =
        MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("testuser")
            .withPassword("testpassword")

    @Test
    fun `contents should not be affected by objects outside files directory`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = S3RepositoryTestRepo(minio.s3URL, "test-bucket", "testuser", "testpassword")
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

            val testRepo = S3RepositoryTestRepo(minio.s3URL, "test-bucket", minio.userName, minio.password)

            assertThrows<NoSuchBucket> {
                testRepo.open(dispatcher)
            }
        }

    @Test
    fun `should fail on wrong access key`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = S3RepositoryTestRepo(minio.s3URL, "test-bucket", minio.userName + "corruption", minio.password)

            assertThrows<WrongCredentialsException> {
                testRepo.open(dispatcher)
            }
        }

    @Test
    fun `should fail on wrong secret key`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = S3RepositoryTestRepo(minio.s3URL, "test-bucket", minio.userName, minio.password + "corruption")

            assertThrows<WrongCredentialsException> {
                testRepo.open(dispatcher)
            }
        }
}
