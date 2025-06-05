package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.demo.phone
import org.archivekeep.app.core.persistence.platform.demo.usbStickAll
import org.archivekeep.app.core.persistence.platform.demo.usbStickDocuments
import org.archivekeep.app.core.persistence.platform.demo.usbStickMusic
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.fixedFilesystemDirectoryPicker
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.app.ui.utils.filesystem.LocalFilesystemDirectoryPicker
import org.archivekeep.files.repo.files.createFilesRepo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.Path

class AddFileSystemRepositoryDialogTest {
    @JvmField
    @Rule
    var testTempDir: TemporaryFolder = TemporaryFolder()

    private fun mountPoints() =
        listOf(
            MountedFileSystem.MountPoint(
                testTempDir.root.path.toString(),
                "TEST-ROOT",
                "TEST-TMP-DIR",
                "/",
            ),
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyInitFlow() {
        runHighDensityComposeUiTest {
            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    environmentFactory = { scope ->
                        DemoEnvironment(
                            scope,
                            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
                            enableSpeedLimit = false,
                            mountPoints = mountPoints(),
                        )
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    CompositionLocalProvider(
                        LocalFilesystemDirectoryPicker provides
                            fixedFilesystemDirectoryPicker(testTempDir.newFolder("local-archives/test-repo").path),
                    ) {
                        AddFileSystemRepositoryDialog(
                            intendedStorageType = FileSystemStorageType.LOCAL,
                        ).render(onClose = {})
                    }
                }
            }

            run {
                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-01-input.png")

                onNodeWithText("local-archives/test-repo", true).assertExists()
                onNodeWithText("The directory is not a repository, yet. Continue to initialize it as an archive repository.").assertExists()
                onNodeWithText("Storage is used for the first time, and it will be marked as local.").assertExists()
            }

            run {
                onNodeWithText("Init").performClick()

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-02-finished.png")

                onNodeWithText("Directory initialized successfully as repository.").assertExists()
                onNodeWithText("Added successfully.").assertExists()
                onNodeWithText("Init").assertDoesNotExist()
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyAddFlow() {
        runHighDensityComposeUiTest {
            val repoPath = testTempDir.newFolder("local-archives/test-repo").path
            createFilesRepo(Path(repoPath))

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    environmentFactory = { scope ->
                        DemoEnvironment(
                            scope,
                            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
                            enableSpeedLimit = false,
                            mountPoints = mountPoints(),
                        )
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    CompositionLocalProvider(
                        LocalFilesystemDirectoryPicker provides fixedFilesystemDirectoryPicker(repoPath),
                    ) {
                        AddFileSystemRepositoryDialog(
                            intendedStorageType = FileSystemStorageType.LOCAL,
                        ).render(onClose = {})
                    }
                }
            }

            run {
                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-01-input.png")

                onNodeWithText("local-archives/test-repo", true).assertExists()
                onNodeWithText("Repository can be added.").assertExists()
                onNodeWithText("Storage is used for the first time, and it will be marked as local.").assertExists()
            }

            run {
                onNodeWithText("Add").performClick()

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-02-finished.png")

                onNodeWithText("Added successfully.").assertExists()
                onNodeWithText("Add").assertDoesNotExist()
            }
        }
    }
}
