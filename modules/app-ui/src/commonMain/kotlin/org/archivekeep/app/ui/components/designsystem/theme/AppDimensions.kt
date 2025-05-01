package org.archivekeep.app.ui.components.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val viewPadding: Dp,
    val viewItemSpacing: Dp,
    val viewExtraPaddingForScrollbar: Dp,
    val gridSpacingHorizontal: Dp,
    val gridSpacingVertical: Dp,
    val dialogContainerPadding: Dp,
)

internal expect fun viewExtraPaddingForScrollbar(): Dp

val smallDimensions =
    AppDimensions(
        viewPadding = 18.dp,
        viewItemSpacing = 18.dp,
        viewExtraPaddingForScrollbar = viewExtraPaddingForScrollbar(),
        gridSpacingHorizontal = 10.dp,
        gridSpacingVertical = 10.dp,
        dialogContainerPadding = 16.dp,
    )

val largeDimensions =
    AppDimensions(
        viewPadding = 24.dp,
        viewItemSpacing = 24.dp,
        viewExtraPaddingForScrollbar = viewExtraPaddingForScrollbar(),
        gridSpacingHorizontal = 12.dp,
        gridSpacingVertical = 14.dp,
        dialogContainerPadding = 24.dp,
    )
