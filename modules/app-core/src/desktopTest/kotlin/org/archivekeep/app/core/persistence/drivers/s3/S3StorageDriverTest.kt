package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.archivekeep.app.core.createTestBucket
import org.archivekeep.app.core.operations.RequiresCredentialsException
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedWalletDataStore
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3Repository
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.exceptions.WrongCredentialsException
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI

@Testcontainers
class S3StorageDriverTest {
    private val bucketName = "test-bucket"

    @Container
    var minio: MinIOContainer =
        MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("testuser")
            .withPassword("testpassword")

    @Test
    fun discoveryShouldThrowErrorOnDiscoverWithMissingCredentials() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            shouldThrow<RequiresCredentialsException> {
                driver.discoverRepository(
                    RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    null,
                )
            }
        }

    @Test
    fun discoveryShouldThrowErrorOnDiscoverWithWrongCredentials() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            shouldThrow<WrongCredentialsException> {
                driver.discoverRepository(
                    RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    BasicAuthCredentials("wrong_user", "wrong_password"),
                )
            }
        }

    @Test
    fun discoveryShouldReturnCanBeInitializedOnNonInitialized() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            val result =
                driver.discoverRepository(
                    RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    BasicAuthCredentials("testuser", "testpassword"),
                )

            result.javaClass shouldBe S3RepositoryLocation.LocationCanBeInitialized::class.java
        }

    @Test
    fun discoveryShouldReturnPlainRepositoryIfPresent() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            S3Repository.create(
                URI.create(minio.s3URL),
                "aa",
                StaticCredentialsProvider {
                    accessKeyId = "testuser"
                    secretAccessKey = "testpassword"
                },
                bucketName,
            )

            val result =
                driver.discoverRepository(
                    RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    BasicAuthCredentials("testuser", "testpassword"),
                )

            result.javaClass shouldBe S3RepositoryLocation.ContainingS3Repository::class.java
        }

    @Test
    fun discoveryShouldReturnEncryptedRepositoryIfPresent() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            EncryptedS3Repository.create(
                URI.create(minio.s3URL),
                "aa",
                StaticCredentialsProvider {
                    accessKeyId = "testuser"
                    secretAccessKey = "testpassword"
                },
                bucketName,
                password = "the-contents-password",
            )

            val result =
                driver.discoverRepository(
                    RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    BasicAuthCredentials("testuser", "testpassword"),
                )

            result.javaClass shouldBe S3RepositoryLocation.ContainingEncryptedS3Repository::class.java
        }

    interface InnerTestScope {
        val testScope: TestScope
        val driver: S3StorageDriver
    }

    private fun runDriverTest(testBody: suspend InnerTestScope.() -> Unit): TestResult =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)

            val env = DemoEnvironment(scope, false, emptyList())

            val credentialsStore: CredentialsStore = CredentialsInProtectedWalletDataStore(env.walletDataStore)

            val driver = S3StorageDriver(scope, credentialsStore)

            testBody(
                object : InnerTestScope {
                    override val testScope = this@runTest
                    override val driver = driver
                },
            )
        }
}
