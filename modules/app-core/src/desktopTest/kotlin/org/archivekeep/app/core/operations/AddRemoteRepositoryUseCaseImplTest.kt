package org.archivekeep.app.core.operations

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.archivekeep.app.core.createTestBucket
import org.archivekeep.app.core.domain.repositories.DefaultRepositoryService
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedWalletDataStore
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.driver.s3.S3Repository
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Testcontainers
class AddRemoteRepositoryUseCaseImplTest {
    private val bucketName = "test-bucket"

    @Container
    var minio: MinIOContainer =
        MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("testuser")
            .withPassword("testpassword")

    @Test
    fun `repository should be auto re-opened after added (even without credentials preservation)`(
        @TempDir t: File,
    ) = runTest {
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

        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)

        val env = DemoEnvironment(scope, false, emptyList())

        val drivers =
            listOf(
                FileSystemStorageDriver(scope, env.fileStores, env.credentialsStore),
                GRPCStorageDriver(scope, env.credentialsStore),
                S3StorageDriver(scope, env.credentialsStore, ioDispatcher = dispatcher),
            ).associateBy { it.ID }
        val repositoryService =
            DefaultRepositoryService(
                scope,
                drivers,
                env.credentialsStore,
                env.registry,
                env.repositoryIndexMemory,
                env.repositoryMetadataMemory,
            )

        val useCase =
            AddRemoteRepositoryUseCaseImpl(
                repositoryService,
                env.registry,
                env.credentialsStore,
                drivers,
            )

        useCase.addS3(minio.s3URL, "test-bucket", minio.userName, minio.password, false)

        val expectedResultURI = RepositoryURI("s3", "${minio.s3URL}|test-bucket")

        env.registry.registeredRepositories.first() shouldContainExactly
            setOf(
                RegisteredRepository(expectedResultURI),
            )

        env.credentialsStore.inMemoryCredentials.first() shouldContainExactly
            mapOf(
                expectedResultURI to BasicAuthCredentials(minio.userName, minio.password),
            )

        // assume slow refresh and re-subscribe
        advanceTimeByAndWaitForIdle(1.minutes)

        // TODO - improve - theoretically, this still can depend on race condition:
        // - maybe test explicitly it's stored as a strong reference (keep open) in some collection,
        // - maybe test for credentials/session store in in-memory wallet

        val openState =
            repositoryService
                .getRepository(expectedResultURI)
                .optionalAccessorFlow
                .map { listOf(it) }
                .runningReduce { accumulator, value -> accumulator + value }
                .stateIn(scope)

        // expect to start loading again as nothing consumes the shared flow now
        openState.value.first().javaClass shouldBe OptionalLoadable.Loading.javaClass

        eventually(3.seconds) {
            // should auto-open with in-memory remembered credentials
            openState.value.last().javaClass shouldBe OptionalLoadable.LoadedAvailable::class.java
        }
    }

    @Test
    fun `credentials should be preserved`(
        @TempDir t: File,
    ) = runTest {
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

        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)

        val env = DemoEnvironment(scope, false, emptyList())

        val drivers =
            listOf(
                FileSystemStorageDriver(scope, env.fileStores, env.credentialsStore),
                GRPCStorageDriver(scope, env.credentialsStore),
                S3StorageDriver(scope, env.credentialsStore, ioDispatcher = dispatcher),
            ).associateBy { it.ID }
        val repositoryService =
            DefaultRepositoryService(
                scope,
                drivers,
                env.credentialsStore,
                env.registry,
                env.repositoryIndexMemory,
                env.repositoryMetadataMemory,
            )

        val useCase =
            AddRemoteRepositoryUseCaseImpl(
                repositoryService,
                env.registry,
                env.credentialsStore,
                drivers,
            )

        env.walletDataStore.create("wallet-password")

        useCase.addS3(minio.s3URL, "test-bucket", minio.userName, minio.password, true)

        val expectedResultURI = RepositoryURI("s3", "${minio.s3URL}|test-bucket")

        env.registry.registeredRepositories.first() shouldContainExactly
            setOf(
                RegisteredRepository(expectedResultURI),
            )

        env.walletDataStore
            .data
            .firstLoadedOrNullOnErrorOrLocked()
            .shouldNotBeNull {
                this.repositoryCredentials shouldContainExactly
                    setOf(
                        CredentialsInProtectedWalletDataStore.PersistedRepositoryCredentials(
                            expectedResultURI,
                            Json.encodeToString(
                                BasicAuthCredentials(
                                    minio.userName,
                                    minio.password,
                                ),
                            ),
                        ),
                    )
            }
    }
}

fun TestScope.advanceTimeByAndWaitForIdle(delayTime: Duration) {
    this.testScheduler.advanceTimeByAndWaitForIdle(delayTime)
}

fun TestCoroutineScheduler.advanceTimeByAndWaitForIdle(delayTime: Duration) {
    advanceTimeBy(delayTime)
    advanceUntilIdle()
}
