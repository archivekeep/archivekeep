package org.archivekeep.app.ui.dialogs.repository.management

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.demo.Documents
import org.archivekeep.app.core.persistence.platform.demo.LaptopHDD
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.usbStickAllUnassociated
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class AssociateRepositoryDialogTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyPath() {
        runHighDensityComposeUiTest {
            var closed = false
            var demoEnvironment: DemoEnvironment? = null

            val subjectAtTestURI = Documents.inStorage(usbStickAllUnassociated.reference).uri

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    environmentFactory = { scope ->
                        demoEnvironment =
                            DemoEnvironment(
                                scope,
                                physicalMediaData =
                                    listOf(
                                        LaptopSSD,
                                        LaptopHDD,
                                        usbStickAllUnassociated,
                                    ),
                                enableSpeedLimit = false,
                            )
                        demoEnvironment!!
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    AssociateRepositoryDialog(subjectAtTestURI)
                        .render(onClose = { closed = true })
                }
            }

            run {
                saveTestingDialogContainerBitmap("dialogs/associate-repository/input-01.png")

                onNodeWithText("Associate").assertIsNotEnabled()
                onNodeWithText("Laptop / SSD / Documents", true).assertExists()
            }

            run {
                onNodeWithText("Laptop / SSD / Documents", true).performClick()

                saveTestingDialogContainerBitmap("dialogs/associate-repository/input-02.png")

                onNodeWithText("Associate").assertIsEnabled()
            }

            runBlocking {
                onNodeWithText("Associate").performClick()

                eventually(2.seconds) {
                    assertEquals(true, closed)
                    assertEquals(
                        "a-documents",
                        demoEnvironment!!
                            .repo(subjectAtTestURI)!!
                            .repo
                            .getMetadata()
                            .associationGroupId,
                    )
                }
            }
        }
    }
}
