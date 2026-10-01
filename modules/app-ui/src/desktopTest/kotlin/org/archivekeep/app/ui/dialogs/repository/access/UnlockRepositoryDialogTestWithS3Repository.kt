package org.archivekeep.app.ui.dialogs.repository.access

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.runBlocking
import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.performClickTextInput
import org.archivekeep.app.ui.utils.S3RepositoryTestRepo
import org.archivekeep.app.ui.utils.env.runHighDensityComposeUiTestWithDemoEnv
import org.archivekeep.app.ui.utils.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.utils.screenshots.setContentInDialogScreenshotContainer
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.stateIn
import org.junit.Rule
import org.junit.Test
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.seconds

class UnlockRepositoryDialogTestWithS3Repository {
    private val bucketName = "test-bucket"
    private val accessKey = "foo"
    private val secretKey = "bar"

    @JvmField
    @Rule
    var s3Mock: S3MockContainer =
        S3MockContainer(DockerImageName.parse("adobe/s3mock:4.3.0"))

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyPath() {
        runHighDensityComposeUiTestWithDemoEnv(
            physicalMediaData = emptyList(),
        ) { env ->
            val testRepo = S3RepositoryTestRepo(s3Mock.httpEndpoint, bucketName, accessKey, secretKey)

            val subjectAtTestURI = S3RepositoryURIData(testRepo.s3URL, testRepo.bucketName).toURI()

            runBlocking {
                testRepo.createBucket()
                testRepo.create()

                env.services.registry.updateRepositories {
                    it + setOf(RegisteredRepository(uri = subjectAtTestURI))
                }
            }

            setContentInDialogScreenshotContainer {
                ApplicationProviders(env.services) {
                    UnlockRepositoryDialog(subjectAtTestURI, onUnlock = {}).render(onClose = { })
                }
            }

            fun onSubmitNode() = onNodeWithText("Authenticate")

            run {
                saveTestingContainerBitmap("dialogs/unlock-repository/01-initial.png")

                onNodeWithText("Authentication is needed to access test-bucket repository.").assertExists()
                onSubmitNode().assertIsNotEnabled()
            }

            run {
                onNodeWithText("Enter username ...").performClickTextInput(testRepo.accessKey)
                onNodeWithText("Enter password ...").performClickTextInput(testRepo.secretKey)

                saveTestingContainerBitmap("dialogs/unlock-repository/02-input-provided.png")

                onSubmitNode().assertIsEnabled()
            }

            runBlocking {
                onSubmitNode().performClick()

                val accessor =
                    env
                        .services
                        .repositoryService
                        .getRepository(subjectAtTestURI)
                        .optionalAccessorFlow
                        .stateIn(env.scope, SharingStarted.Eagerly)

                eventually(2.seconds) {
                    saveTestingContainerBitmap("dialogs/unlock-repository/03-final.png")

                    accessor.value.javaClass shouldBe OptionalLoadable.LoadedAvailable::class.java

                    onNodeWithText("Repository test-bucket is now unlocked.").assertExists()
                    onSubmitNode().assertDoesNotExist()
                }
            }
        }
    }
}
