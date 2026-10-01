package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import dev.zacsweers.metro.createGraphFactory
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
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3Repository
import org.archivekeep.utils.exceptions.WrongCredentialsException
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.firstFinishedLoading
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.net.URI
import kotlin.time.Duration.Companion.seconds

@Testcontainers
class S3StorageDriverTest {
    private val bucketName = "test-bucket"
    private val accessKey = "foo"
    private val secretKey = "bar"
    private val region = "us-east-1"

    @Container
    var s3Mock: S3MockContainer =
        S3MockContainer(DockerImageName.parse("adobe/s3mock:4.3.0"))

    @Test
    fun discoveryShouldAskForCredentials() =
        runDriverTest {
            createTestBucket(s3Mock, bucketName)

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${s3Mock.httpEndpoint}|test-bucket"),
                    ).contentsStateFlow
                    .firstFinishedLoading()

            result.javaClass shouldBe NeedsUnlock::class.java
            (result as NeedsUnlock).unlockRequest.javaClass shouldBe UserCredentialsRequest::class.java
        }

    // TODO: either fix test or alter behaviour of implementation
    @Disabled("Difference in behaviour of S3 test/mock implementation")
    @Test
    fun discoveryShouldThrowErrorOnWrongCredentials() =
        runDriverTest {
            createTestBucket(s3Mock, bucketName)

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${s3Mock.httpEndpoint}|test-bucket"),
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
            createTestBucket(s3Mock, bucketName)

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${s3Mock.httpEndpoint}|test-bucket"),
                    ).internalStateFlow
                    .transform {
                        if (it is NeedsUnlock) {
                            (it.unlockRequest as UserCredentialsRequest).tryOpen(
                                BasicAuthCredentials(accessKey, secretKey),
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
            createTestBucket(s3Mock, bucketName)

            S3Repository.create(
                URI.create(s3Mock.httpEndpoint),
                region,
                StaticCredentialsProvider {
                    accessKeyId = accessKey
                    secretAccessKey = secretKey
                },
                bucketName,
            )

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${s3Mock.httpEndpoint}|test-bucket"),
                    ).internalStateFlow
                    .transform {
                        if (it is NeedsUnlock) {
                            (it.unlockRequest as UserCredentialsRequest).tryOpen(
                                BasicAuthCredentials(accessKey, secretKey),
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
            createTestBucket(s3Mock, bucketName)

            EncryptedS3Repository.create(
                URI.create(s3Mock.httpEndpoint),
                region,
                StaticCredentialsProvider {
                    accessKeyId = accessKey
                    secretAccessKey = secretKey
                },
                bucketName,
                password = "the-contents-password",
            )

            val result =
                driver
                    .openLocation(
                        RepositoryURI("s3", "${s3Mock.httpEndpoint}|test-bucket"),
                    ).internalStateFlow
                    .transform {
                        if (it is NeedsUnlock) {
                            (it.unlockRequest as UserCredentialsRequest).tryOpen(
                                BasicAuthCredentials(accessKey, secretKey),
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

            val env =
                createGraphFactory<DemoApplicationServices.Factory>().create(
                    scope,
                    serviceWorkDispatcher = dispatcher,
                    physicalMediaData = emptyList(),
                    enableSpeedLimit = false,
                    storagesOverride = emptyList(),
                )

            val driver =
                env.storageDrivers.values
                    .filterIsInstance<S3StorageDriver>()
                    .first()

            testBody(
                object : InnerTestScope {
                    override val testScope = this@runTest
                    override val driver = driver
                },
            )
        }
}
