package org.archivekeep.app.ui.dialogs.repository.management

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.zacsweers.metro.createGraphFactory
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.core.persistence.platform.demo.Documents
import org.archivekeep.app.core.persistence.platform.demo.LaptopHDD
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.usbStickAllUnassociated
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProvidersFromCore
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class AssociateRepositoryDialogTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyPath() {
        runHighDensityComposeUiTest {
            var closed = false
            var demoApplicationServices: DemoApplicationServices? = null

            val subjectAtTestURI = Documents.uriInStorage(usbStickAllUnassociated.reference)

            setContentInDialogScreenshotContainer {
                ApplicationProvidersFromCore(
                    coreApplicationServicesFactory = { scope, dispatcher ->
                        demoApplicationServices =
                            createGraphFactory<DemoApplicationServices.Factory>().create(
                                scope,
                                dispatcher,
                                physicalMediaData =
                                    listOf(
                                        LaptopSSD,
                                        LaptopHDD,
                                        usbStickAllUnassociated,
                                    ),
                                enableSpeedLimit = false,
                            )

                        demoApplicationServices
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
                        (
                            demoApplicationServices!!
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
