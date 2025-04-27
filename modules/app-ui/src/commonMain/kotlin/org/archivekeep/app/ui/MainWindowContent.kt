package org.archivekeep.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.base.layout.ApplicationNavigationLayout
import org.archivekeep.app.ui.components.base.layout.MainWindowLayout
import org.archivekeep.app.ui.components.base.layout.calculateNavigationLocation
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.LocalStorageOperationsLaunchers
import org.archivekeep.app.ui.domain.wiring.LocalWalletOperationLaunchers
import org.archivekeep.app.ui.domain.wiring.OverlayDialogRenderer
import org.archivekeep.app.ui.domain.wiring.archiveOperationLaunchersAsDialogs
import org.archivekeep.app.ui.domain.wiring.rememberWalletOperationLaunchersAsDialogs
import org.archivekeep.app.ui.domain.wiring.storageOperationsLaunchersAsDialogs

@Composable
fun MainWindowContent(
    isFloating: Boolean,
    windowSizeClass: WindowSizeClass,
    onCloseRequest: (() -> Unit)?,
) {
    val dialogRenderer = remember { OverlayDialogRenderer() }

    val archiveOperationLaunchers = remember { archiveOperationLaunchersAsDialogs(dialogRenderer) }
    val storageOperationsLaunchers = remember { storageOperationsLaunchersAsDialogs(dialogRenderer) }
    val walletOperationLaunchers = rememberWalletOperationLaunchersAsDialogs(dialogRenderer)

    val applicationNavigationLayout = windowSizeClass.calculateNavigationLocation()

    AppTheme(
        small = windowSizeClass.widthSizeClass < WindowWidthSizeClass.Expanded,
    ) {
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
                    if (isFloating) {
                        if (applicationNavigationLayout != ApplicationNavigationLayout.RAIL_BAR) {
                            BorderStroke(1.dp, verticalGradient)
                        } else {
                            BorderStroke(1.dp, horizontalGradient)
                        }
                    } else {
                        null
                    },
            ) {
                MainWindowLayout(
                    applicationNavigationLayout = applicationNavigationLayout,
                    onCloseRequest = onCloseRequest,
                )
            }

            dialogRenderer.render()
        }
    }
}

private val verticalGradient =
    Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.05f to Color.Transparent,
        0.12f to Color.Black.copy(alpha = 0.10f),
        0.89f to Color.Black.copy(alpha = 0.15f),
        1.0f to Color.Black.copy(alpha = 0.20f),
    )

private val horizontalGradient =
    Brush.horizontalGradient(
        0.0f to Color.Transparent,
        0.05f to Color.Transparent,
        0.12f to Color.Black.copy(alpha = 0.10f),
        0.89f to Color.Black.copy(alpha = 0.15f),
        1.0f to Color.Black.copy(alpha = 0.20f),
    )
