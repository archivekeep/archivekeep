package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.demo.phone
import org.archivekeep.app.core.persistence.platform.demo.usbStickAll
import org.archivekeep.app.core.persistence.platform.demo.usbStickDocuments
import org.archivekeep.app.core.persistence.platform.demo.usbStickMusic
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.WalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.newServiceWorkExecutorDispatcher
import org.archivekeep.app.ui.performClickTextInput
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.app.ui.utils.S3RepositoryTestRepo
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3Repository
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.MinIOContainer
import java.net.URI

@OptIn(ExperimentalTestApi::class)
class AddRemoteRepositoryDialogTestWithS3 {
    private val bucketName = "test-bucket"

    @JvmField
    @Rule
    var minio: MinIOContainer =
        MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("testuser")
            .withPassword("testpassword")

    private fun registeredRepositoryForMock() =
        RegisteredRepository(
            S3RepositoryURIData(minio.s3URL, bucketName).toURI(),
            null,
            null,
        )

    @Test
    fun showsErrorOnWrongCredentials() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(minio.s3URL, bucketName, "testuser", "testpassword")
                testRepo.createBucket()
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(minio.s3URL)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("wrong_key")
            onNodeWithText("Secret key").performClickTextInput("wrong_secret")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-04-done.png")

            onAllNodesWithText("WrongCredentialsException", substring = true).assertCountEquals(2)

            runBlocking {
                services.environment.registry.registeredRepositories
                    .first()
            } shouldNotContain registeredRepositoryForMock()
        }
    }

    @Test
    fun initializesAsPlain() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(minio.s3URL, bucketName, "testuser", "testpassword")
                testRepo.createBucket()
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(minio.s3URL)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("testuser")
            onNodeWithText("Secret key").performClickTextInput("testpassword")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-04-needs-init.png")

            onNodeWithText("Init").assertIsNotEnabled()

            onNodeWithText("Plain objects", substring = true).performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-05-can-init.png")

            onNodeWithText("Init").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-06-init-result.png")

            onNodeWithText("Remote repository successfully added").assertExists()

            runBlocking {
                services.environment.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    @Test
    fun initializesAsEncrypted() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(minio.s3URL, bucketName, "testuser", "testpassword")
                testRepo.createBucket()
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(minio.s3URL)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("testuser")
            onNodeWithText("Secret key").performClickTextInput("testpassword")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-04-needs-init.png")

            onNodeWithText("Init").assertIsNotEnabled()

            onNodeWithText("Encrypted (custom format)").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-05-before-password.png")

            onNodeWithText("Enter password ...").assertExists()
            onNodeWithText("Verify password ...").assertExists()

            onNodeWithText("Enter password ...").performClickTextInput("test-create-password")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-06-first-password.png")

            onNodeWithText("Verify password ...").performClickTextInput("test-create-password")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-07-second-password.png")

            onNodeWithText("Init").assertIsEnabled()

            onNodeWithText("Init").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-08-completed.png")

            onNodeWithText("Remote repository successfully added").assertExists()

            runBlocking {
                services.environment.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    @Test
    fun addPlainRepository() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(minio.s3URL, bucketName, "testuser", "testpassword")
                testRepo.createBucket()

                S3Repository.create(
                    URI.create(minio.s3URL),
                    "aa",
                    StaticCredentialsProvider {
                        accessKeyId = "testuser"
                        secretAccessKey = "testpassword"
                    },
                    bucketName,
                )
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(minio.s3URL)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("testuser")
            onNodeWithText("Secret key").performClickTextInput("testpassword")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-04-completed.png")

            onNodeWithText("Remote repository successfully added").assertExists()

            runBlocking {
                services.environment.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    @Test
    fun addEncryptedRepository() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(minio.s3URL, bucketName, "testuser", "testpassword")
                testRepo.createBucket()

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
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(minio.s3URL)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("testuser")
            onNodeWithText("Secret key").performClickTextInput("testpassword")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-04-completed.png")

            onNodeWithText("Remote repository successfully added").assertExists()

            runBlocking {
                services.environment.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    class TestContext(
        val composeUiTest: SkikoComposeUiTest,
        val services: ApplicationServices,
    ) : SemanticsNodeInteractionsProvider by composeUiTest {
        fun saveTestingDialogContainerBitmap(filename: String) {
            composeUiTest.saveTestingDialogContainerBitmap(filename)
        }
    }

    private fun runDriverTest(block: TestContext.() -> Unit) {
        runHighDensityComposeUiTest {
            val job = SupervisorJob()
            val scope = CoroutineScope(job)
            val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()
            val environment =
                DemoEnvironment(
                    scope,
                    physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
                    enableSpeedLimit = false,
                )
            val services = ApplicationServices(serviceWorkDispatcher, scope, environment)

            try {
                setContentInDialogScreenshotContainer {
                    ApplicationProviders(
                        applicationServices = services,
                        applicationMetadata = PropertiesApplicationMetadata(),
                    ) {
                        CompositionLocalProvider(
                            LocalWalletOperationLaunchers provides
                                WalletOperationLaunchers(
                                    ensureWalletForWrite = { false },
                                    openUnlockWallet = { error("Shouldn't be called") },
                                ),
                        ) {
                            AddRemoteRepositoryDialog().render(onClose = {})
                        }
                    }
                }

                TestContext(
                    this@runHighDensityComposeUiTest,
                    services,
                ).apply(block)
            } finally {
                job.cancel()
                serviceWorkDispatcher.cancel()
            }
        }
    }
}
