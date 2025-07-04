package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.runBlocking
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
import org.archivekeep.app.ui.performClickTextInput
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.app.ui.utils.filesystem.LocalFilesystemDirectoryPicker
import org.archivekeep.files.repo.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.repo.files.createFilesRepo
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

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

            runBlocking {
                onNodeWithText("Init").performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-02-finished.png")

                    onNodeWithText("Directory initialized successfully as repository.").assertExists()
                    onNodeWithText("Added successfully.").assertExists()
                    onNodeWithText("Init").assertDoesNotExist()
                }
            }
        }
    }

    @Ignore
    @Test
    fun testHappyInitEncryptedFlow() {
        TODO()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyAddFlowForPlainRepository() {
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

            runBlocking {
                onNodeWithText("Add").performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-02-finished.png")

                    onNodeWithText("Added successfully.").assertExists()
                    onNodeWithText("Add").assertDoesNotExist()
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyAddFlowForEncryptedRepository() {
        runHighDensityComposeUiTest {
            val repoPath = testTempDir.newFolder("local-archives/test-encrypted-repo").path
            runBlocking {
                EncryptedFileSystemRepository.create(Path(repoPath), "test-password")
            }

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
                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-encrypted-01-after-selection.png")

                onNodeWithText("local-archives/test-encrypted-repo", true).assertExists()
                onNodeWithText("The repository is encrypted, and password protected.").assertExists()
                onNodeWithText("Enter password to access it:").assertExists()
                onNodeWithText("Unlock").assertIsNotEnabled()
                onNodeWithText("Add").assertIsNotEnabled()
            }

            run {
                onNodeWithText("Enter password ...").performClickTextInput("test-password")

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-encrypted-02-input-password.png")

                onNodeWithText("Unlock").assertIsEnabled()
                onNodeWithText("Add").assertIsNotEnabled()
            }

            runBlocking {
                onNodeWithText("Unlock").performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-encrypted-03-unlocked.png")

                    onNodeWithText("Successfully unlocked.").assertExists()
                    onNodeWithText("Unlock").assertDoesNotExist()
                    onNodeWithText("Add").assertIsEnabled()
                }
            }

            runBlocking {
                onNodeWithText("Add").performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-encrypted-04-finished.png")

                    onNodeWithText("Added successfully.").assertExists()
                    onNodeWithText("Add").assertDoesNotExist()
                }
            }
        }
    }
}
