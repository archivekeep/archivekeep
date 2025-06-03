package org.archivekeep.app.ui.dialogs.repository.registry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCase
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.demo.phone
import org.archivekeep.app.core.persistence.platform.demo.usbStickAll
import org.archivekeep.app.core.persistence.platform.demo.usbStickDocuments
import org.archivekeep.app.core.persistence.platform.demo.usbStickMusic
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.ui.dialogs.testing.saveTestingDialogContainerBitmap
import org.archivekeep.app.desktop.ui.dialogs.testing.setContentInDialogScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.LocalOperationFactory
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.junit.Test

class AddRemoteRepositoryDialogTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testS3() {
        runHighDensityComposeUiTest {
            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    environmentFactory = { scope ->
                        DemoEnvironment(
                            scope,
                            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
                            enableSpeedLimit = false,
                        )
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    CompositionLocalProvider(
                        LocalOperationFactory provides
                            LocalOperationFactory.current.override(
                                AddRemoteRepositoryUseCase::class.java,
                                NoOpAddRemoteRepositoryUseCase(),
                            ),
                    ) {
                        AddRemoteRepositoryDialog().render(onClose = {})
                    }
                }
            }

            onNodeWithText("S3").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/input-s3-01.png")

            onNodeWithText("Endpoint URL").performClickTextInput("https://s3.endpoint.nas.lan")
            onNodeWithText("Bucket name").performClickTextInput("documents")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/input-s3-02.png")

            onNodeWithText("Access key").performClickTextInput("the_access_key")
            onNodeWithText("Secret key").performClickTextInput("the_secret_key")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/input-s3-03.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/input-s3-04-done.png")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testOther() {
        runHighDensityComposeUiTest {
            setContentInDialogScreenshotContainer {
                ApplicationProviders(
                    environmentFactory = { scope ->
                        DemoEnvironment(
                            scope,
                            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
                            enableSpeedLimit = false,
                        )
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    CompositionLocalProvider(
                        LocalOperationFactory provides
                            LocalOperationFactory.current.override(
                                AddRemoteRepositoryUseCase::class.java,
                                NoOpAddRemoteRepositoryUseCase(),
                            ),
                    ) {
                        AddRemoteRepositoryDialog().render(onClose = {})
                    }
                }
            }

            onNodeWithText("Other").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/input-grpc-01.png")

            onNodeWithText("Remote repository URL").performClickTextInput("grpc://private-nas.lan:24202/archives/1")

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/input-grpc-02.png")

            onNodeWithText("Add").performClick()

            saveTestingDialogContainerBitmap("dialogs/add-remote-repository/input-grpc-03-done.png")
        }
    }
}

class NoOpAddRemoteRepositoryUseCase : AddRemoteRepositoryUseCase {
    override suspend fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
    ) {}
}

private fun SemanticsNodeInteraction.performClickTextInput(text: String) {
    performClick()
    performTextInput(text)
}
