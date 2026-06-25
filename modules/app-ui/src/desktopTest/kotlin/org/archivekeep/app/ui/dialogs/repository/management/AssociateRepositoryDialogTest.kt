package org.archivekeep.app.ui.dialogs.repository.management

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.platform.demo.Documents
import org.archivekeep.app.core.persistence.platform.demo.LaptopHDD
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.usbStickAllUnassociated
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.env.runHighDensityComposeUiTestWithDemoEnv
import org.archivekeep.app.ui.utils.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.utils.screenshots.setContentInDialogScreenshotContainer
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class AssociateRepositoryDialogTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyPath() {
        runHighDensityComposeUiTestWithDemoEnv(
            physicalMediaData =
                listOf(
                    LaptopSSD,
                    LaptopHDD,
                    usbStickAllUnassociated,
                ),
        ) { env ->
            var closed = false

            val subjectAtTestURI = Documents.uriInStorage(usbStickAllUnassociated.reference)

            setContentInDialogScreenshotContainer {
                ApplicationProviders(env.services) {
                    AssociateRepositoryDialog(subjectAtTestURI)
                        .render(onClose = { closed = true })
                }
            }

            run {
                saveTestingContainerBitmap("dialogs/associate-repository/input-01.png")

                onNodeWithText("Associate").assertIsNotEnabled()
                onNodeWithText("Laptop / SSD / Documents", true).assertExists()
            }

            run {
                onNodeWithText("Laptop / SSD / Documents", true).performClick()

                saveTestingContainerBitmap("dialogs/associate-repository/input-02.png")

                onNodeWithText("Associate").assertIsEnabled()
            }

            runBlocking {
                onNodeWithText("Associate").performClick()

                eventually(2.seconds) {
                    assertEquals(true, closed)
                    assertEquals(
                        "a-documents",
                        (
                            env.services
                                .repositoryService
                                .getRepository(subjectAtTestURI)
                                .metadataFlowWithCaching
                                .value
                                as OptionalLoadable.LoadedAvailable
                        ).value
                            .associationGroupId,
                    )
                }
            }
        }
    }
}
