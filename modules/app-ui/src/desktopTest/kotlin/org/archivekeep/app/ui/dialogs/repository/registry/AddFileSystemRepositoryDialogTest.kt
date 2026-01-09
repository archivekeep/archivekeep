package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.driver.filesystem.files.FilesRepo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.Path
import kotlin.test.assertNotNull
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
                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-plain-01-selection.png")

                onNodeWithText("local-archives/test-repo", true).assertExists()
                onNodeWithText("The directory is not a repository, yet. Continue to initialize it as an archive repository.").assertExists()
                onNodeWithText("Storage is used for the first time, and it will be marked as local.").assertExists()
            }

            runBlocking {
                onNodeWithText("Init").performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-plain-02-finished.png")

                    onNodeWithText("Directory initialized successfully as repository.").assertExists()
                    onNodeWithText("Added successfully.").assertExists()
                    onNodeWithText("Init").assertDoesNotExist()
                    onNodeWithText("Close").assertIsEnabled()
                }

                // should not fail
                assertNotNull(FilesRepo.openOrNull(testTempDir.root.resolve("local-archives/test-repo").toPath()))
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyInitEncryptedFlow() {
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

            fun onInitSubmitButton() = onNodeWithText("Init")

            run {
                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-encrypted-01-selection.png")

                onNodeWithText("local-archives/test-repo", true).assertExists()
                onNodeWithText("The directory is not a repository, yet. Continue to initialize it as an archive repository.").assertExists()
                onNodeWithText("Storage is used for the first time, and it will be marked as local.").assertExists()
            }

            run {
                onNodeWithText("Encrypted (custom format)").performClick()

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-encrypted-02-switch-to-encrypted.png")

                onNodeWithText("Enter password ...").assertExists()
                onNodeWithText("Verify password ...").assertExists()
                onInitSubmitButton().assertIsNotEnabled()
            }

            run {
                onNodeWithText("Enter password ...").performClickTextInput("test-create-password")

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-encrypted-03-1-first-password.png")

                onInitSubmitButton().assertIsNotEnabled()
            }

            run {
                onNodeWithText("Verify password ...").performClickTextInput("wrong-password")

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-encrypted-03-2-wrong-password.png")

                onInitSubmitButton().assertIsNotEnabled()
            }

            run {
                onNodeWithTag("Verify password ...").run {
                    performClick()
                    performTextClearance()
                    performTextInput("test-create-password")
                }

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-encrypted-03-3-verify-password.png")

                onInitSubmitButton().assertIsEnabled()
            }

            runBlocking {
                onNodeWithText("Init").performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/init-encrypted-04-finished.png")

                    onNodeWithText("Directory initialized successfully as repository.").assertExists()
                    onNodeWithText("Added successfully.").assertExists()
                    onNodeWithTag("Enter password ...").assertIsNotEnabled()
                    onNodeWithTag("Verify password ...").assertIsNotEnabled()
                    onNodeWithText("Init").assertDoesNotExist()
                    onNodeWithText("Close").assertIsEnabled()
                }

                // should not fail
                EncryptedFileSystemRepository.openAndUnlock(
                    testTempDir.root.resolve("local-archives/test-repo").toPath(),
                    "test-create-password",
                )
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyAddFlowForPlainRepository() {
        runHighDensityComposeUiTest {
            val repoPath = testTempDir.newFolder("local-archives/test-repo").path
            FilesRepo.create(Path(repoPath))

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
                EncryptedFileSystemRepository.create(Path(repoPath), "test-add-password")
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
                onNodeWithText("Enter password ...").performClickTextInput("test-add-password")

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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testIncorrectPasswordAddFlowForEncryptedRepository() {
        runHighDensityComposeUiTest {
            val repoPath = testTempDir.newFolder("local-archives/test-encrypted-repo").path
            runBlocking {
                EncryptedFileSystemRepository.create(Path(repoPath), "test-add-password")
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
                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-wrong-password-encrypted-01-after-selection.png")

                onNodeWithText("local-archives/test-encrypted-repo", true).assertExists()
                onNodeWithText("The repository is encrypted, and password protected.").assertExists()
                onNodeWithText("Enter password to access it:").assertExists()
                onNodeWithText("Unlock").assertIsNotEnabled()
                onNodeWithText("Add").assertIsNotEnabled()
            }

            run {
                onNodeWithText("Enter password ...").performClickTextInput("some-wrong-password")

                saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-wrong-password-encrypted-02-input-password.png")

                onNodeWithText("Unlock").assertIsEnabled()
                onNodeWithText("Add").assertIsNotEnabled()
            }

            runBlocking {
                onNodeWithText("Unlock").performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/add-filesystem-repository/add-wrong-password-encrypted-03-failed.png")

                    onNodeWithText("Entered password isn't correct.").assertExists()
                    onNodeWithText("Unlock").assertIsEnabled()
                    onNodeWithText("Add").assertIsNotEnabled()
                }
            }
        }
    }
}
