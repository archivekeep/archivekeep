package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalStorageOperationsLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.OverlayDialogRenderer
import org.archivekeep.app.desktop.domain.wiring.archiveOperationLaunchersAsDialogs
import org.archivekeep.app.desktop.domain.wiring.rememberWalletOperationLaunchersAsDialogs
import org.archivekeep.app.desktop.domain.wiring.storageOperationsLaunchersAsDialogs
import org.archivekeep.app.desktop.ui.designsystem.styles.DesktopAppTheme
import org.archivekeep.app_desktop.generated.resources.Res
import org.archivekeep.app_desktop.generated.resources.ic_app
import org.jetbrains.compose.resources.painterResource

@Composable
fun (ApplicationScope).MainWindow() {
    val dialogRenderer = remember { OverlayDialogRenderer() }

    val windowState =
        rememberWindowState(
            width = 1000.dp,
            height = 700.dp,
        )

    val archiveOperationLaunchers = remember { archiveOperationLaunchersAsDialogs(dialogRenderer) }
    val storageOperationsLaunchers = remember { storageOperationsLaunchersAsDialogs(dialogRenderer) }
    val walletOperationLaunchers = rememberWalletOperationLaunchersAsDialogs(dialogRenderer)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "ArchiveKeep",
        // note: this seems to not be respected by GNOME and generic icon is shown for the app,
        //       but _NET_WM_ICON is set and icon can be retrieved using xprop, see:
        //       https://unix.stackexchange.com/questions/48860/how-to-dump-the-icon-of-a-running-x-program
        icon = painterResource(Res.drawable.ic_app),
        undecorated = true,
        transparent = true,
    ) {
        DesktopAppTheme {
            CompositionLocalProvider(
                LocalArchiveOperationLaunchers provides archiveOperationLaunchers,
                LocalStorageOperationsLaunchers provides storageOperationsLaunchers,
                LocalWalletOperationLaunchers provides walletOperationLaunchers,
            ) {
                Surface(
                    shape =
                        if (windowState.placement == WindowPlacement.Floating) {
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 2.dp)
                        } else {
                            RectangleShape
                        },
                    border =
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                0.0f to Color.Transparent,
                                0.3f to Color.Transparent,
                                1.0f to Color.Black.copy(alpha = 0.15f),
                            ),
                        ),
                ) {
                    MainWindowLayout(onCloseRequest = ::exitApplication)

                    dialogRenderer.render(window)
                }
            }
        }
    }
}
