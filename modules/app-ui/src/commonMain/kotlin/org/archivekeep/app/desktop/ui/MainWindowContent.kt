package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalStorageOperationsLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.desktop.domain.wiring.OverlayDialogRenderer
import org.archivekeep.app.desktop.domain.wiring.archiveOperationLaunchersAsDialogs
import org.archivekeep.app.desktop.domain.wiring.rememberWalletOperationLaunchersAsDialogs
import org.archivekeep.app.desktop.domain.wiring.storageOperationsLaunchersAsDialogs
import org.archivekeep.app.desktop.ui.designsystem.styles.DesktopAppTheme

@Composable
fun MainWindowContent(
    isFloating: Boolean,
    windowSizeClass: WindowSizeClass,
    onCloseRequest: () -> Unit,
) {
    val dialogRenderer = remember { OverlayDialogRenderer() }

    val archiveOperationLaunchers = remember { archiveOperationLaunchersAsDialogs(dialogRenderer) }
    val storageOperationsLaunchers = remember { storageOperationsLaunchersAsDialogs(dialogRenderer) }
    val walletOperationLaunchers = rememberWalletOperationLaunchersAsDialogs(dialogRenderer)

    DesktopAppTheme {
        CompositionLocalProvider(
            LocalArchiveOperationLaunchers provides archiveOperationLaunchers,
            LocalStorageOperationsLaunchers provides storageOperationsLaunchers,
            LocalWalletOperationLaunchers provides walletOperationLaunchers,
        ) {
            Surface(
                shape =
                    if (isFloating) {
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
                MainWindowLayout(
                    windowSizeClass = windowSizeClass,
                    onCloseRequest = onCloseRequest,
                )

                dialogRenderer.render()
            }
        }
    }
}
