package org.archivekeep.app.desktop.ui.dialogs.repository.operations.addpush

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.ReadyAddPushProcess
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInBackBlaze
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInHDDA
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInLaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInSSDKeyChain
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.addpush.AddAndPushRepoDialogViewModel.VMState
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.utils.loading.Loadable
import org.junit.Test

class AddAndPushRepoDialogScreenshotTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun makeScreenshot() {
        runHighDensityComposeUiTest {
            setContentInDialogScreenshotContainer {
                val allMoves =
                    listOf(
                        AddOperation.PreparationResult.Move("Invoices/2024/01.PDF", "Contracting/Invoices/2024/01.PDF"),
                        AddOperation.PreparationResult.Move("Invoices/2024/02.PDF", "Contracting/Invoices/2024/02.PDF"),
                    )
                val allNewFiles =
                    listOf(
                        "2024/08/01.JPG",
                        "2024/08/02.JPG",
                        "something-unwanted.txt",
                    )
                val allDestinations = setOf(DocumentsInSSDKeyChain.uri)

                val dialog = AddAndPushRepoDialog(DocumentsInLaptopSSD.uri)

                dialog.renderDialogCard(
                    VMState(
                        repoName = "Documents",
                        ReadyAddPushProcess(
                            AddOperation.PreparationResult(
                                newFiles = allNewFiles,
                                moves = allMoves,
                                missingFiles = emptyList(),
                                errorFiles = emptyMap(),
                            ),
                            launch = {},
                        ),
                        selectedFilenames =
                            mutableStateOf(
                                setOf(
                                    "2024/08/01.JPG",
                                    "2024/08/02.JPG",
                                ),
                            ),
                        selectedMoves = mutableStateOf(allMoves.toSet()),
                        selectedDestinationRepositories = mutableStateOf(allDestinations),
                        otherRepositoryCandidates =
                            Loadable.Loaded(
                                listOf(
                                    DocumentsInSSDKeyChain.storageRepository,
                                    DocumentsInHDDA.storageRepository,
                                    DocumentsInBackBlaze.storageRepository,
                                ),
                            ),
                        onCancel = {},
                        onClose = {},
                    ),
                )
            }

            saveTestingContainerBitmap("dialogs/add-and-push/example.png")
        }
    }
}
