package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val labelTextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)

@Composable
fun LabelText(text: String) {
    Text(text, style = labelTextStyle)
}
