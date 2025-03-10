package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val DialogTitleTopPadding = 6.dp
val DialogTitleContentSpacing = 12.dp

val DialogContentButtonsSpacing = 16.dp

val DialogContentPadding = 16.dp

val DialogTitleStyle =
    TextStyle.Default.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.1.sp,
    )
