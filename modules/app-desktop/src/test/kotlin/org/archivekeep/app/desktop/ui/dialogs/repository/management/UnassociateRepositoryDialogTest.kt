package org.archivekeep.app.desktop.ui.dialogs.repository.management

import androidx.compose.ui.test.ExperimentalTestApi
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInLaptopSSD
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.junit.Test

class UnassociateRepositoryDialogScreenshotTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun makeScreenshot() {
        runHighDensityComposeUiTest {
            setContentInDialogScreenshotContainer {
                val dialog = UnassociateRepositoryDialog(DocumentsInLaptopSSD.uri)

                dialog.renderDialogCard(
                    UnassociateRepositoryDialog.State(
                        KnownStorage(DocumentsInLaptopSSD.storage.uri, null, emptyList()),
                        RepositoryInformation(null, "A Repo"),
                        onLaunch = {},
                        onClose = {},
                    ),
                )
            }

            saveTestingContainerBitmap("dialogs/unassociate-repository/example.png")
        }
    }
}
