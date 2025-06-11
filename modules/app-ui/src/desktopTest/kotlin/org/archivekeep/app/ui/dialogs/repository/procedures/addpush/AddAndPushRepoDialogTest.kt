package org.archivekeep.app.ui.dialogs.repository.procedures.addpush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.core.persistence.platform.demo.BackBlaze
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.demo.Documents
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInLaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.hddB
import org.archivekeep.app.core.persistence.platform.demo.ssdKeyChain
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AddAndPushRepoDialogScreenshotTest {
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
                                                    Documents
                                                        .withNewContents {
                                                            addUncommitted("Contracting/Invoices/2024/01.PDF", "Invoices/2024/01.PDF")
                                                            addUncommitted("Contracting/Invoices/2024/02.PDF", "Invoices/2024/02.PDF")
                                                            addMissing("Invoices/2024/01.PDF")
                                                            addMissing("Invoices/2024/02.PDF")
                                                            addUncommitted("2024/08/01.JPG")
                                                            addUncommitted("2024/08/02.JPG")
                                                            addUncommitted("something-unwanted.txt")
                                                        }.localInMemoryFactory(),
                                                ),
                                        ),
                                        ssdKeyChain.copy(repositories = listOf(Documents.withNewContents { })),
                                        hddB.copy(repositories = listOf(Documents.withNewContents { })),
                                    ),
                                onlineStoragesData =
                                    listOf(
                                        BackBlaze.copy(repositories = listOf(Documents.withNewContents {})),
                                    ),
                                enableSpeedLimit = false,
                            )
                        demoEnvironment!!
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    AddAndPushRepoDialog(
                        DocumentsInLaptopSSD.uri,
                    ).render(onClose = {})
                }
            }

            runBlocking {
                fun title() = onNodeWithText("Documents - add and push")

                val itemsToClick =
                    listOf(
                        "Invoices/2024/01.PDF -> Contracting/Invoices/2024/01.PDF",
                        "Invoices/2024/02.PDF -> Contracting/Invoices/2024/02.PDF",
                        "2024/08/01.JPG",
                        "2024/08/02.JPG",
                        "HDD B",
                        "Backblaze S3 (planned)",
                    )

                eventually(2.seconds) {
                    title().assertExists()
                    itemsToClick.forEach { onNodeWithText(it).assertExists() }
                }

                itemsToClick.forEach { onNodeWithText(it).performClick() }

                // move away mouse
                title().performMouseInput { moveTo(Offset.Zero) }

                saveTestingDialogContainerBitmap("dialogs/add-and-push/example.png")
            }
        }
    }
}
