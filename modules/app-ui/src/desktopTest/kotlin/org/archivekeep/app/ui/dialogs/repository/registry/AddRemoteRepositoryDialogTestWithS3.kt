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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.persistence.platform.demo.phone
import org.archivekeep.app.core.persistence.platform.demo.usbStickAll
import org.archivekeep.app.core.persistence.platform.demo.usbStickDocuments
import org.archivekeep.app.core.persistence.platform.demo.usbStickMusic
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.WalletOperationLaunchers
import org.archivekeep.app.ui.performClickTextInput
import org.archivekeep.app.ui.utils.S3RepositoryTestRepo
import org.archivekeep.app.ui.utils.env.runHighDensityComposeUiTestWithDemoEnv
import org.archivekeep.app.ui.utils.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.utils.screenshots.setContentInDialogScreenshotContainer
import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3Repository
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.testcontainers.utility.DockerImageName
import java.net.URI

@OptIn(ExperimentalTestApi::class)
class AddRemoteRepositoryDialogTestWithS3 {
    private val bucketName = "test-bucket"
    private val accessKey = "foo"
    private val secretKey = "bar"

    @JvmField
    @Rule
    var s3Mock: S3MockContainer =
        S3MockContainer(DockerImageName.parse("adobe/s3mock:4.3.0"))

    private fun registeredRepositoryForMock() =
        RegisteredRepository(
            S3RepositoryURIData(s3Mock.httpEndpoint, bucketName).toURI(),
            null,
            null,
        )

    // TODO: either fix test or alter behaviour of implementation
    @Ignore("Difference in behaviour of S3 test/mock implementation")
    @Test
    fun showsErrorOnWrongCredentials() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey)
                testRepo.createBucket()
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(s3Mock.httpEndpoint)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("wrong_key")
            onNodeWithText("Secret key").performClickTextInput("wrong_secret")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/wrong-credentials/input-s3-04-done.png")

            onAllNodesWithText("WrongCredentialsException", substring = true).assertCountEquals(2)

            runBlocking {
                services.registry.registeredRepositories
                    .first()
            } shouldNotContain registeredRepositoryForMock()
        }
    }

    @Test
    fun initializesAsPlain() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey)
                testRepo.createBucket()
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(s3Mock.httpEndpoint)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-plain/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput(accessKey)
            onNodeWithText("Secret key").performClickTextInput(secretKey)

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
                services.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    @Test
    fun initializesAsEncrypted() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey)
                testRepo.createBucket()
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(s3Mock.httpEndpoint)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/init-as-encrypted/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput(accessKey)
            onNodeWithText("Secret key").performClickTextInput(secretKey)

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
                services.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    @Test
    fun addPlainRepository() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey)
                testRepo.createBucket()

                S3Repository.create(
                    URI.create(s3Mock.httpEndpoint),
                    "us-east-1",
                    StaticCredentialsProvider {
                        accessKeyId = accessKey
                        secretAccessKey = secretKey
                    },
                    bucketName,
                )
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(s3Mock.httpEndpoint)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput(accessKey)
            onNodeWithText("Secret key").performClickTextInput(secretKey)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-plain/input-s3-04-completed.png")

            onNodeWithText("Remote repository successfully added").assertExists()

            runBlocking {
                services.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    @Test
    fun addEncryptedRepository() {
        runDriverTest {
            runBlocking {
                val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey)
                testRepo.createBucket()

                EncryptedS3Repository.create(
                    URI.create(s3Mock.httpEndpoint),
                    "us-east-1",
                    StaticCredentialsProvider {
                        accessKeyId = accessKey
                        secretAccessKey = secretKey
                    },
                    bucketName,
                    password = "the-contents-password",
                )
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput(s3Mock.httpEndpoint)
            onNodeWithText("Bucket name").performClickTextInput(bucketName)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput(accessKey)
            onNodeWithText("Secret key").performClickTextInput(secretKey)

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/s3-test/add-encrypted/input-s3-04-completed.png")

            onNodeWithText("Remote repository successfully added").assertExists()

            runBlocking {
                services.registry.registeredRepositories
                    .first()
            } shouldContain registeredRepositoryForMock()
        }
    }

    class TestContext(
        val composeUiTest: SkikoComposeUiTest,
        val services: ApplicationServices,
    ) : SemanticsNodeInteractionsProvider by composeUiTest {
        fun saveTestingDialogContainerBitmap(filename: String) {
            composeUiTest.saveTestingContainerBitmap(filename)
        }
    }

    private fun runDriverTest(block: TestContext.() -> Unit) {
        runHighDensityComposeUiTestWithDemoEnv(
            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
        ) { env ->
            setContentInDialogScreenshotContainer {
                ApplicationProviders(env.services) {
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
                this@runHighDensityComposeUiTestWithDemoEnv,
                env.services,
            ).apply(block)
        }
    }
}
