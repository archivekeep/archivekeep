package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.archivekeep.app.core.api.repository.location.UserCredentialsRequest
import org.archivekeep.app.core.createTestBucket
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedWalletDataStore
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3Repository
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.exceptions.WrongCredentialsException
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.firstFinishedLoading
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI
import kotlin.time.Duration.Companion.seconds

@Testcontainers
class S3StorageDriverTest {
    private val bucketName = "test-bucket"

    @Container
    var minio: MinIOContainer =
        MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("testuser")
            .withPassword("testpassword")

    @Test
    fun discoveryShouldAskForCredentials() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    ).contentsStateFlow
                    .firstFinishedLoading()

            result.javaClass shouldBe NeedsUnlock::class.java
            (result as NeedsUnlock).unlockRequest.javaClass shouldBe UserCredentialsRequest::class.java
        }

    @Test
    fun discoveryShouldThrowErrorOnWrongCredentials() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    ).contentsStateFlow
                    .firstFinishedLoading()

            shouldThrow<WrongCredentialsException> {
                ((result as NeedsUnlock).unlockRequest as UserCredentialsRequest).tryOpen(
                    BasicAuthCredentials("wrong_user", "wrong_password"),
                    UnlockOptions(false, false),
                )
            }
        }

    @Test
    fun discoveryShouldReturnCanBeInitializedOnNonInitialized() =
        runDriverTest {
            createTestBucket(minio, bucketName)

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    ).internalStateFlow
                    .transform {
                        if (it is NeedsUnlock) {
                            (it.unlockRequest as UserCredentialsRequest).tryOpen(
                                BasicAuthCredentials("testuser", "testpassword"),
                                UnlockOptions(false, false),
                            )
                        } else if (it is OptionalLoadable.LoadedAvailable) {
                            emit(it)
                        }
                    }.first()
                    .value

            result.javaClass shouldBe S3StorageDriver.InnerState.LocationCanBeInitialized::class.java
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
                driver
                    .openLocation(
                        RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    ).internalStateFlow
                    .transform {
                        if (it is NeedsUnlock) {
                            (it.unlockRequest as UserCredentialsRequest).tryOpen(
                                BasicAuthCredentials("testuser", "testpassword"),
                                UnlockOptions(false, false),
                            )
                        } else if (it is OptionalLoadable.LoadedAvailable) {
                            emit(it)
                        }
                    }.first()
                    .value

            result.javaClass shouldBe S3StorageDriver.InnerState.PlainS3Repository::class.java
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
                driver
                    .openLocation(
                        RepositoryURI("s3", "${minio.s3URL}|test-bucket"),
                    ).internalStateFlow
                    .transform {
                        if (it is NeedsUnlock) {
                            (it.unlockRequest as UserCredentialsRequest).tryOpen(
                                BasicAuthCredentials("testuser", "testpassword"),
                                UnlockOptions(false, false),
                            )
                        } else if (it is OptionalLoadable.LoadedAvailable) {
                            emit(it)
                        }
                    }.first()
                    .value

            result.javaClass shouldBe S3StorageDriver.InnerState.EncryptedS3Repository::class.java
        }

    interface InnerTestScope {
        val testScope: TestScope
        val driver: S3StorageDriver
    }

    private fun runDriverTest(testBody: suspend InnerTestScope.() -> Unit): TestResult =
        runTest(
            timeout = 10.seconds,
        ) {
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
