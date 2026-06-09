package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import dev.zacsweers.metro.createGraphFactory
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.Photos
import org.archivekeep.app.core.persistence.platform.demo.hddB
import org.archivekeep.app.core.persistence.platform.photosAdjustmentA
import org.archivekeep.app.core.persistence.platform.photosAdjustmentB
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.createApplicationServices
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.files.driver.fixtures.FixtureRepoBuilder
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class UploadToRepoDialogScreenshotTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPreparationAndInput() {
        runHighDensityComposeUiTest {
            var demoApplicationServices: DemoApplicationServices? = null

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServicesFactory = { scope, dispatcher ->
                        demoApplicationServices =
                            createGraphFactory<DemoApplicationServices.Factory>().create(
                                scope,
                                dispatcher,
                                physicalMediaData =
                                    listOf(
                                        LaptopSSD.copy(
                                            repositories = listOf(Photos.withContents(FixtureRepoBuilder::photosAdjustmentA)),
                                        ),
                                        hddB.copy(
                                            repositories = listOf(Photos.withContents(FixtureRepoBuilder::photosAdjustmentB)),
                                        ),
                                    ),
                                enableSpeedLimit = false,
                            )

                        createApplicationServices(demoApplicationServices)
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    UploadToRepoDialog(
                        Photos.uriInStorage(hddB.reference),
                        Photos.uriInStorage(LaptopSSD.reference),
                    ).render(onClose = {})
                }
            }

            runBlocking {
                val itemsToSelect =
                    listOf(
                        "2024/6/1.JPG",
                        "2024/6/2.JPG",
                        "2024/6/3.JPG",
                        "2024/6/4.JPG",
                    )

                eventually(2.seconds) {
                    onNodeWithText("Photos - copy to", true).assertExists()

                    itemsToSelect.forEach { onNodeWithText(it).assertExists() }
                }

                itemsToSelect.forEach { onNodeWithText(it).performClick() }

                // move away mouse
                onNodeWithText("Photos - copy to", true).performMouseInput { moveTo(Offset.Zero) }

                saveTestingDialogContainerBitmap("dialogs/sync/upload-example.png")
            }
        }
    }
}
