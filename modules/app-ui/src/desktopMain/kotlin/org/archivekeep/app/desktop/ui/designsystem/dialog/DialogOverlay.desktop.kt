package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
actual fun dialogProperties(): DialogProperties =
    DialogProperties(
        usePlatformInsets = false,
        usePlatformDefaultWidth = false,
    )

actual fun dialogBottomPaddingHack(): Dp = 0.dp
