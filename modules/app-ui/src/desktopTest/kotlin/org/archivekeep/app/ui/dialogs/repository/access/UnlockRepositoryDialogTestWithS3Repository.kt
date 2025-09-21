package org.archivekeep.app.ui.dialogs.repository.access

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.newServiceWorkExecutorDispatcher
import org.archivekeep.app.ui.performClickTextInput
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.app.ui.utils.S3RepositoryTestRepo
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.stateIn
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.MinIOContainer
import kotlin.time.Duration.Companion.seconds

class UnlockRepositoryDialogTestWithS3Repository {
    @JvmField
    @Rule
    var minio: MinIOContainer =
        MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z")
            .withUserName("testuser")
            .withPassword("testpassword")

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyPath() {
        runHighDensityComposeUiTest {
            val scope = CoroutineScope(SupervisorJob())
            val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()
            val demoEnvironment =
                DemoEnvironment(
                    scope,
                    physicalMediaData = emptyList(),
                    enableSpeedLimit = false,
                )
            val services = ApplicationServices(serviceWorkDispatcher, scope, demoEnvironment)

            val testRepo = S3RepositoryTestRepo(minio.s3URL, "test-bucket", "testuser", "testpassword")

            val subjectAtTestURI = S3RepositoryURIData(testRepo.s3URL, testRepo.bucketName).toURI()

            runBlocking {
                testRepo.createBucket()
                testRepo.create()

                demoEnvironment.registry.updateRepositories {
                    it + setOf(RegisteredRepository(uri = subjectAtTestURI))
                }
            }

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServices = services,
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    UnlockRepositoryDialog(subjectAtTestURI, onUnlock = {}).render(onClose = { })
                }
            }

            fun onSubmitNode() = onNodeWithText("Authenticate")

            run {
                saveTestingDialogContainerBitmap("dialogs/unlock-repository/01-initial.png")

                onNodeWithText("Authentication is needed to access test-bucket repository.").assertExists()
                onSubmitNode().assertIsNotEnabled()
            }

            run {
                onNodeWithText("Enter username ...").performClickTextInput(testRepo.accessKey)
                onNodeWithText("Enter password ...").performClickTextInput(testRepo.secretKey)

                saveTestingDialogContainerBitmap("dialogs/unlock-repository/02-input-provided.png")

                onSubmitNode().assertIsEnabled()
            }

            runBlocking {
                onSubmitNode().performClick()

                val accessor =
                    services
                        .repoService
                        .getRepository(subjectAtTestURI)
                        .optionalAccessorFlow
                        .stateIn(scope, SharingStarted.Eagerly)

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/unlock-repository/03-final.png")

                    accessor.value.javaClass shouldBe OptionalLoadable.LoadedAvailable::class.java

                    onNodeWithText("Repository test-bucket is now unlocked.").assertExists()
                    onSubmitNode().assertDoesNotExist()
                }
            }
        }
    }
}
