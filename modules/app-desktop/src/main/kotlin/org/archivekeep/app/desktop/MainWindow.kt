package org.archivekeep.app.desktop

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import org.archivekeep.app.ui.MainWindowContent
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme
import org.archivekeep.app.ui.domain.wiring.LocalComposeWindow
import org.archivekeep.app.ui.domain.wiring.LocalOptionalComposeWindow
import org.archivekeep.app.ui.utils.DefaultMainWindowHeight
import org.archivekeep.app.ui.utils.DefaultMainWindowWidth
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.ic_app
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
            val windowSizeClass = calculateWindowSizeClass()

            AppTheme(
                small = windowSizeClass.widthSizeClass < WindowWidthSizeClass.Expanded,
            ) {
                MainWindowContent(
                    isFloating = windowState.placement == WindowPlacement.Floating,
                    windowSizeClass = windowSizeClass,
                    ::exitApplication,
                )
            }
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
