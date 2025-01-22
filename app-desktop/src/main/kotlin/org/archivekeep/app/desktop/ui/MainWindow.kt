package org.archivekeep.app.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalStorageOperationsLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.OverlayDialogRenderer
import org.archivekeep.app.desktop.domain.wiring.archiveOperationLaunchersAsDialogs
import org.archivekeep.app.desktop.domain.wiring.rememberWalletOperationLaunchersAsDialogs
import org.archivekeep.app.desktop.domain.wiring.storageOperationsLaunchersAsDialogs
import org.archivekeep.app.desktop.ui.designsystem.styles.DesktopAppTheme

@Composable
fun (ApplicationScope).MainWindow() {
    val dialogRenderer = remember { OverlayDialogRenderer() }

    val archiveOperationLaunchers = remember { archiveOperationLaunchersAsDialogs(dialogRenderer) }
    val storageOperationsLaunchers = remember { storageOperationsLaunchersAsDialogs(dialogRenderer) }
    val walletOperationLaunchers = rememberWalletOperationLaunchersAsDialogs(dialogRenderer)

    Window(
        onCloseRequest = ::exitApplication,
        state =
            rememberWindowState(
                width = 1000.dp,
                height = 700.dp,
            ),
    ) {
        DesktopAppTheme {
            CompositionLocalProvider(
                LocalArchiveOperationLaunchers provides archiveOperationLaunchers,
                LocalStorageOperationsLaunchers provides storageOperationsLaunchers,
                LocalWalletOperationLaunchers provides walletOperationLaunchers,
            ) {
                MainWindowLayout()

                dialogRenderer.render(window)
            }
        }
    }
}
