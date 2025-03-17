package org.archivekeep.app.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import org.archivekeep.app.desktop.domain.wiring.LocalComposeWindow
import org.archivekeep.app.desktop.domain.wiring.LocalOptionalComposeWindow
import org.archivekeep.app_desktop.generated.resources.Res
import org.archivekeep.app_desktop.generated.resources.ic_app
import org.jetbrains.compose.resources.painterResource

val DefaultMainWindowWidth = 1050.dp
val DefaultMainWindowHeight = 800.dp

@Composable
fun (ApplicationScope).MainWindow() {
    val windowState =
        rememberWindowState(
            width = DefaultMainWindowWidth,
            height = DefaultMainWindowHeight,
        )

    ensureMinimumSize(windowState)

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
        CompositionLocalProvider(
            LocalComposeWindow provides window,
            LocalOptionalComposeWindow provides window,
        ) {
            MainWindowContent(
                isFloating = windowState.placement == WindowPlacement.Floating,
                ::exitApplication,
            )
        }
    }
}

private fun ensureMinimumSize(windowState: WindowState) {
    val minWidth = 360.dp
    val minHeight = 120.dp

    if (windowState.size.width < minWidth || windowState.size.height < minHeight) {
        windowState.size =
            DpSize(
                width = windowState.size.width.let { if (it < minWidth) minWidth else it },
                height = windowState.size.height.let { if (it < minHeight) minHeight else it },
            )
    }
}
