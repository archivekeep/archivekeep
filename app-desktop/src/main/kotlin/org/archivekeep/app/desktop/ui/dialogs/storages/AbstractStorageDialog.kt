package org.archivekeep.app.desktop.ui.dialogs.storages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.buildAnnotatedString
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.collectLoadableFlow

abstract class AbstractStorageDialog<VM : AbstractStorageDialog.IVM>(
    val uri: StorageURI,
) : Dialog {
    interface IVM {
        val title: String
    }

    @Composable
    abstract fun createVM(storage: Storage): VM

    @Composable
    abstract fun renderContent(vm: VM)

    @Composable
    abstract fun RowScope.renderButtons(
        onClose: () -> Unit,
        vm: VM,
    )

    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val storageService = LocalStorageService.current
        val storageLoadable =
            remember(storageService, uri) {
                storageService.allStorages
                    .mapLoadedData { it.firstOrNull { it.uri == uri } }
            }.collectLoadableFlow()

        DialogOverlayCard(onDismissRequest = onClose) {
            LoadableGuard(
                storageLoadable,
            ) { storage ->

                if (storage != null) {
                    val vm = createVM(storage)
                    DialogInnerContainer(
                        remember {
                            buildAnnotatedString {
                                append(vm.title)
                            }
                        },
                        content = {
                            renderContent(vm)
                        },
                        bottomContent = {
                            DialogButtonContainer {
                                renderButtons(onClose, vm)
                            }
                        },
                    )
                } else {
                    DialogInnerContainer(
                        buildAnnotatedString {
                            append("Storage `$uri` not found")
                        },
                        content = {
                        },
                        bottomContent = {
                            DialogDismissButton("Dismiss", onClick = onClose)
                        },
                    )
                }
            }
        }
    }
}
