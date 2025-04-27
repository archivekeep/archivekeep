package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

actual fun dialogProperties(): DialogProperties =
    DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = true,
    )

// TODO - find solution: this extra represents roughly status bar height, and is need to not overflow below screen
actual fun dialogBottomPaddingHack(): Dp = 60.dp
