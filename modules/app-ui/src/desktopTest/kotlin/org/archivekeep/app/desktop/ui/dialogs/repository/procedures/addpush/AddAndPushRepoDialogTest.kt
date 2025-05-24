package org.archivekeep.app.desktop.ui.dialogs.repository.procedures.addpush

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInBackBlaze
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInHDDA
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInLaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInSSDKeyChain
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.ReadyAddPushProcess
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.dialogs.repository.procedures.addpush.AddAndPushRepoDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.addpush.AddAndPushRepoDialogViewModel.VMState
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
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
                        IndexUpdateProcedure.PreparationResult.Move(
                            "TODO",
                            10,
                            "Invoices/2024/01.PDF",
                            "Contracting/Invoices/2024/01.PDF",
                        ),
                        IndexUpdateProcedure.PreparationResult.Move(
                            "TODO",
                            10,
                            "Invoices/2024/02.PDF",
                            "Contracting/Invoices/2024/02.PDF",
                        ),
                    )
                val allNewFiles =
                    listOf(
                        IndexUpdateProcedure.PreparationResult.NewFile(
                            "2024/08/01.JPG",
                            null,
                        ),
                        IndexUpdateProcedure.PreparationResult.NewFile(
                            "2024/08/02.JPG",
                            null,
                        ),
                        IndexUpdateProcedure.PreparationResult.NewFile(
                            "something-unwanted.txt",
                            null,
                        ),
                    )
                val allDestinations = setOf(DocumentsInSSDKeyChain.uri)

                val dialog = AddAndPushRepoDialog(DocumentsInLaptopSSD.uri)

                dialog.renderDialogCard(
                    VMState(
                        repoName = "Documents",
                        ReadyAddPushProcess(
                            IndexUpdateProcedure.PreparationResult(
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
                                ).let { selected ->
                                    allNewFiles.filter { it.fileName in selected }.toSet()
                                },
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
