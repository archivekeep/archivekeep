package org.archivekeep.app.core.operations

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.collections.shouldContainExactly
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.createTestBucket
import org.archivekeep.app.core.domain.repositories.DefaultRepositoryService
import org.archivekeep.app.core.domain.storages.KnownStorageService
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedDataStore
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.PasswordProtectedJoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
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
    fun `repository should be auto re-opened after added`(
        @TempDir t: File,
    ) = runTest {
        createTestBucket(minio, bucketName)

        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher)

        val env = DemoEnvironment(scope, false, emptyList())

        val walletDataStore =
            PasswordProtectedJoseStorage(
                t.resolve("wallet-datastore").toPath(),
                Json.serializersModule.serializer(),
                defaultValueProducer = { Credentials(emptySet()) },
            )

        val credentialsStore: CredentialsStore = CredentialsInProtectedDataStore(walletDataStore)

        val repositoryService =
            DefaultRepositoryService(
                scope,
                listOf(
                    FileSystemStorageDriver(scope, env.fileStores),
                    GRPCStorageDriver(scope, credentialsStore),
                    S3StorageDriver(scope, credentialsStore, ioDispatcher = dispatcher),
                ).associateBy { it.ID },
                env.registry,
                env.repositoryIndexMemory,
                env.repositoryMetadataMemory,
            )

        val knownStorageService = KnownStorageService(scope, env.registry, env.fileStores)

        val useCase =
            AddRemoteRepositoryUseCaseImpl(
                repositoryService,
                env.registry,
                env.fileStores,
                knownStorageService,
            )

        useCase.addS3(minio.s3URL, "test-bucket", minio.userName, minio.password)

        val expectedResultURI = RepositoryURI("s3", "${minio.s3URL}|test-bucket")

        env.registry.registeredRepositories.first() shouldContainExactly
            setOf(
                RegisteredRepository(expectedResultURI),
            )

        // assume slow refresh and re-subscribe
        advanceTimeByAndWaitForIdle(1.minutes)

        println("DONE")

        val openState =
            repositoryService
                .getRepository(expectedResultURI)
                .rawAccessor
                .map { listOf(it) }
                .runningReduce { accumulator, value -> accumulator + value }
                .stateIn(scope)

        // expect to start loading again as nothing consumes the shared flow now
        openState.value.first().javaClass shouldBe ProtectedLoadableResource.Loading.javaClass

        eventually(3.seconds) {
            // should auto-open with in-memory remembered credentials
            openState.value.last().javaClass shouldBe ProtectedLoadableResource.Loaded::class.java
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
