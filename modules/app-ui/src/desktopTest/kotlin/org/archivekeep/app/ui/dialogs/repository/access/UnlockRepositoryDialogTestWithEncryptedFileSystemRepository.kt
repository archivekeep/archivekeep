package org.archivekeep.app.ui.dialogs.repository.access

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.ContentEncryptionPassword
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemRepositoryURIData
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.WalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.newServiceWorkExecutorDispatcher
import org.archivekeep.app.ui.performClickTextInput
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.files.repo.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.firstFinishedLoading
import org.archivekeep.utils.loading.optional.stateIn
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class UnlockRepositoryDialogTestWithEncryptedFileSystemRepository {
    @JvmField
    @Rule
    var testTempDir: TemporaryFolder = TemporaryFolder()

    private fun mountPoints() =
        listOf(
            MountedFileSystem.MountPoint(
                testTempDir.root.path.toString(),
                "TEST-ROOT",
                "TEST-TMP-DIR",
                "",
            ),
        )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyPath() {
        runHighDensityComposeUiTest {
            val scope = CoroutineScope(SupervisorJob())
            val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()
            val demoEnvironment =
                object : DemoEnvironment(
                    scope,
                    physicalMediaData = emptyList(),
                    enableSpeedLimit = false,
                    mountPoints = mountPoints(),
                ) {
                    override val storageDrivers: List<StorageDriver> = listOf(FileSystemStorageDriver(scope, fileStores, credentialsStore))
                }
            val services = ApplicationServices(serviceWorkDispatcher, scope, demoEnvironment)

            val tempRepoPath = testTempDir.newFolder("encrypted-repo")

            val subjectAtTestURI = FileSystemRepositoryURIData(fsUUID = "TEST-TMP-DIR", pathInFS = "encrypted-repo").toURI()

            runBlocking {
                EncryptedFileSystemRepository.create(tempRepoPath.toPath(), "test-password-123")

                demoEnvironment.registry.updateRepositories {
                    it + setOf(RegisteredRepository(uri = subjectAtTestURI))
                }
            }

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServices = services,
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    UnlockRepositoryDialog(subjectAtTestURI, onUnlock = {}).render(onClose = { })
                }
            }

            fun onSubmitNode() = onNodeWithText("Authenticate")

            runBlocking {
                eventually(1.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-password-01-initial.png")

                    onNodeWithText("Password is needed to access encrypted-repo repository.").assertExists()
                    onSubmitNode().assertIsNotEnabled()
                }
            }

            run {
                onNodeWithText("Enter password ...").performClickTextInput("test-password-123")

                saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-password-02-input-provided.png")

                onSubmitNode().assertIsEnabled()
            }

            runBlocking {
                onSubmitNode().performClick()

                val accessor =
                    services
                        .repoService
                        .getRepository(subjectAtTestURI)
                        .optionalAccessorFlow
                        .stateIn(scope, SharingStarted.Eagerly)

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-password-03-final.png")

                    accessor.value.javaClass shouldBe OptionalLoadable.LoadedAvailable::class.java

                    onNodeWithText("Repository encrypted-repo is now unlocked.").assertExists()
                    onSubmitNode().assertDoesNotExist()
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testHappyPathWithRemember() {
        runHighDensityComposeUiTest {
            val scope = CoroutineScope(SupervisorJob())
            val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()
            val demoEnvironment =
                object : DemoEnvironment(
                    scope,
                    physicalMediaData = emptyList(),
                    enableSpeedLimit = false,
                    mountPoints = mountPoints(),
                ) {
                    override val storageDrivers: List<StorageDriver> = listOf(FileSystemStorageDriver(scope, fileStores, credentialsStore))
                }
            val services = ApplicationServices(serviceWorkDispatcher, scope, demoEnvironment)

            runBlocking {
                demoEnvironment.walletDataStore.create("test-wallet-password-${UUID.randomUUID()}")
            }

            val tempRepoPath = testTempDir.newFolder("encrypted-repo")

            val subjectAtTestURI = FileSystemRepositoryURIData(fsUUID = "TEST-TMP-DIR", pathInFS = "encrypted-repo").toURI()

            runBlocking {
                EncryptedFileSystemRepository.create(tempRepoPath.toPath(), "test-password-123")

                demoEnvironment.registry.updateRepositories {
                    it + setOf(RegisteredRepository(uri = subjectAtTestURI))
                }
            }

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServices = services,
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    CompositionLocalProvider(
                        LocalWalletOperationLaunchers provides
                            WalletOperationLaunchers(
                                ensureWalletForWrite = { true },
                                openUnlockWallet = { error("Shouldn't be called") },
                            ),
                    ) {
                        UnlockRepositoryDialog(subjectAtTestURI, onUnlock = {}).render(onClose = { })
                    }
                }
            }

            fun onSubmitNode() = onNodeWithText("Authenticate")

            run {
                saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-and-remember-password-01-initial.png")

                onNodeWithText("Password is needed to access encrypted-repo repository.").assertExists()
                onSubmitNode().assertIsNotEnabled()
            }

            run {
                onNodeWithText("Enter password ...").performClickTextInput("test-password-123")
                onNodeWithText("Remember password").performClick()

                saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-and-remember-password-02-input-provided.png")

                onSubmitNode().assertIsEnabled()
            }

            runBlocking {
                onSubmitNode().performClick()

                val accessor =
                    services
                        .repoService
                        .getRepository(subjectAtTestURI)
                        .optionalAccessorFlow
                        .stateIn(scope, SharingStarted.Eagerly)

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-and-remember-password-03-final.png")

                    accessor.value.javaClass shouldBe OptionalLoadable.LoadedAvailable::class.java

                    onNodeWithText("Repository encrypted-repo is now unlocked.").assertExists()
                    onSubmitNode().assertDoesNotExist()
                }

                assertEquals(
                    JsonPrimitive("test-password-123"),
                    (
                        demoEnvironment
                            .credentialsStore
                            .getRepositorySecretsFlow(subjectAtTestURI)
                            .firstFinishedLoading()
                            as OptionalLoadable.LoadedAvailable
                    ).value[ContentEncryptionPassword],
                    "Should store the password",
                )
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testWrongPassword() {
        runHighDensityComposeUiTest {
            val scope = CoroutineScope(SupervisorJob())
            val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()
            val demoEnvironment =
                object : DemoEnvironment(
                    scope,
                    physicalMediaData = emptyList(),
                    enableSpeedLimit = false,
                    mountPoints = mountPoints(),
                ) {
                    override val storageDrivers: List<StorageDriver> = listOf(FileSystemStorageDriver(scope, fileStores, credentialsStore))
                }
            val services = ApplicationServices(serviceWorkDispatcher, scope, demoEnvironment)

            val tempRepoPath = testTempDir.newFolder("encrypted-repo")

            val subjectAtTestURI = FileSystemRepositoryURIData(fsUUID = "TEST-TMP-DIR", pathInFS = "encrypted-repo").toURI()

            runBlocking {
                EncryptedFileSystemRepository.create(tempRepoPath.toPath(), "test-password-123")

                demoEnvironment.registry.updateRepositories {
                    it + setOf(RegisteredRepository(uri = subjectAtTestURI))
                }
            }

            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServices = services,
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    UnlockRepositoryDialog(subjectAtTestURI, onUnlock = {}).render(onClose = { })
                }
            }

            fun onSubmitNode() = onNodeWithText("Authenticate")

            run {
                saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-incorrect-password-01-initial.png")

                onNodeWithText("Password is needed to access encrypted-repo repository.").assertExists()
                onSubmitNode().assertIsNotEnabled()
            }

            run {
                onNodeWithText("Enter password ...").performClickTextInput("test-password-wrong")

                saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-incorrect-password-02-input-provided.png")

                onSubmitNode().assertIsEnabled()
            }

            runBlocking {
                onSubmitNode().performClick()

                eventually(2.seconds) {
                    saveTestingDialogContainerBitmap("dialogs/unlock-repository/provide-incorrect-password-03-final.png")

                    onNodeWithText("Entered password isn't correct.").assertExists()
                    onSubmitNode().assertIsEnabled()
                }
            }
        }
    }
}
