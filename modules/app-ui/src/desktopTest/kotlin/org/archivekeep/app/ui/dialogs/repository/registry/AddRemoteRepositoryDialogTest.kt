package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createDynamicGraphFactory
import org.archivekeep.app.core.domain.CoreApplicationServices
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.operations.AddRemoteRepositoryOutcome
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCase
import org.archivekeep.app.core.persistence.platform.demo.phone
import org.archivekeep.app.core.persistence.platform.demo.usbStickAll
import org.archivekeep.app.core.persistence.platform.demo.usbStickDocuments
import org.archivekeep.app.core.persistence.platform.demo.usbStickMusic
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.OperationFactory
import org.archivekeep.app.ui.domain.wiring.WalletOperationLaunchers
import org.archivekeep.app.ui.performClickTextInput
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.app.ui.utils.env.runHighDensityComposeUiTestWithDemoEnv
import org.archivekeep.app.ui.utils.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.utils.screenshots.setContentInDialogScreenshotContainer
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.junit.Test

@BindingContainer
class NoOpAddRemoteRepositoryContainer {
    @Provides
    @SingleIn(AppScope::class)
    fun operationFactory(
        repositoryService: RepositoryService,
        env: CoreApplicationServices,
        storageRegistry: StorageRegistry,
        drivers: Map<String, StorageDriver>,
    ): OperationFactory =
        OperationFactory(repositoryService, env, storageRegistry, drivers).override(
            AddRemoteRepositoryUseCase::class.java,
            NoOpAddRemoteRepositoryUseCase(),
        )
}

class AddRemoteRepositoryDialogTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testS3() {
        runHighDensityComposeUiTestWithDemoEnv(
            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
            factory = createDynamicGraphFactory(NoOpAddRemoteRepositoryContainer()),
        ) { env ->
            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServices = env.services,
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    AddRemoteRepositoryDialog().render(onClose = {})
                }
            }

            onNodeWithText("S3").performClick()

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput("https://s3.endpoint.nas.lan")
            onNodeWithText("Bucket name").performClickTextInput("documents")

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("the_access_key")
            onNodeWithText("Secret key").performClickTextInput("the_secret_key")

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-s3-04-done.png")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testS3ShowsWarningOnInsecureProtocol() {
        runHighDensityComposeUiTestWithDemoEnv(
            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
            factory = createDynamicGraphFactory(NoOpAddRemoteRepositoryContainer()),
        ) { env ->
            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServices = env.services,
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    CompositionLocalProvider(
                        LocalWalletOperationLaunchers provides
                            WalletOperationLaunchers(
                                ensureWalletForWrite = { false },
                                openUnlockWallet = { error("Shouldn't be called") },
                            ),
                    ) {
                        AddRemoteRepositoryDialog().render(onClose = {})
                    }
                }
            }

            onNodeWithText("S3").performClick()
            onNodeWithText("Endpoint URL").performClickTextInput("http://s3.insecure.endpoint.nas.lan")

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-s3-insecure-01.png")

            onNodeWithText(
                "Insecure protocol is used for endpoint. This results in plain data being sent over network, that is readable by anyone.",
            ).assertExists()
            onNodeWithText("It is strongly recommended to connect to this server using a VPN you absolutely trust.").assertExists(
                "documents",
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOther() {
        runHighDensityComposeUiTestWithDemoEnv(
            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
            factory = createDynamicGraphFactory(NoOpAddRemoteRepositoryContainer()),
        ) { env ->
            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    applicationServices = env.services,
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    CompositionLocalProvider(
                        LocalWalletOperationLaunchers provides
                            WalletOperationLaunchers(
                                ensureWalletForWrite = { false },
                                openUnlockWallet = { error("Shouldn't be called") },
                            ),
                    ) {
                        AddRemoteRepositoryDialog().render(onClose = {})
                    }
                }
            }

            onNodeWithText("Other").performClick()

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-grpc-01.png")

            onNodeWithText("Remote repository URL").performClickTextInput("grpc://private-nas.lan:24202/archives/1")

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-grpc-02.png")

            onNodeWithText("Add").performClick()

            saveTestingContainerBitmap("dialogs/add-remote-repository/input-grpc-03-done.png")
        }
    }
}

class NoOpAddRemoteRepositoryUseCase : AddRemoteRepositoryUseCase {
    override suspend fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
        rememberCredentials: Boolean,
    ) = AddRemoteRepositoryOutcome.Added
}
