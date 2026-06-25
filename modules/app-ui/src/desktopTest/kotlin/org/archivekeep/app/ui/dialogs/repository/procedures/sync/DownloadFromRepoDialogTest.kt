package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.Photos
import org.archivekeep.app.core.persistence.platform.demo.hddB
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.env.runHighDensityComposeUiTestWithDemoEnv
import org.archivekeep.app.ui.utils.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.utils.screenshots.setContentInDialogScreenshotContainer
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DownloadFromRepoDialogScreenshotTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testPreparationAndInput() {
        runHighDensityComposeUiTestWithDemoEnv(
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
        ) { env ->
            setContentInDialogScreenshotContainer {
                ApplicationProviders(env.services) {
                    DownloadFromRepoDialog(
                        Photos.uriInStorage(hddB.reference),
                        Photos.uriInStorage(LaptopSSD.reference),
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

                saveTestingContainerBitmap("dialogs/sync/download-example.png")
            }
        }
    }
}
