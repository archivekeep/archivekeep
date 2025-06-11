package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.Photos
import org.archivekeep.app.core.persistence.platform.demo.PhotosInHDDB
import org.archivekeep.app.core.persistence.platform.demo.PhotosInLaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.hddB
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DownloadFromRepoDialogScreenshotTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPreparationAndInput() {
        runHighDensityComposeUiTest {
            var demoEnvironment: DemoEnvironment? = null

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    environmentFactory = { scope ->
                        demoEnvironment =
                            DemoEnvironment(
                                scope,
                                physicalMediaData =
                                    listOf(
                                        LaptopSSD.copy(
                                            repositories =
                                                listOf(
                                                    Photos
                                                        .withContents {
                                                            deletePattern("2024/1/.*".toRegex())
                                                            addStored("2024/4/6-duplicate.JPG", "2024/4/6.JPG")
                                                        },
                                                ),
                                        ),
                                        hddB.copy(
                                            repositories =
                                                listOf(
                                                    Photos,
                                                ),
                                        ),
                                    ),
                                enableSpeedLimit = false,
                            )
                        demoEnvironment!!
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    DownloadFromRepoDialog(
                        PhotosInHDDB.uri,
                        PhotosInLaptopSSD.uri,
                    ).render(onClose = {})
                }
            }

            runBlocking {
                val itemsToSelect =
                    listOf(
                        "deduplicate 2024/4/6-duplicate.JPG, 2024/4/6.JPG to keep only 2024/4/6.JPG",
                        "2024/1/3.JPG",
                        "2024/1/4.JPG",
                        "2024/1/7.JPG",
                    )

                eventually(2.seconds) {
                    onNodeWithText("copy from", true).assertExists()

                    itemsToSelect.forEach { onNodeWithText(it).assertExists() }
                }

                itemsToSelect.forEach { onNodeWithText(it).performClick() }

                // move away mouse
                onNodeWithText("copy from", true).performMouseInput { moveTo(Offset.Zero) }

                saveTestingDialogContainerBitmap("dialogs/sync/download-example.png")
            }
        }
    }
}
