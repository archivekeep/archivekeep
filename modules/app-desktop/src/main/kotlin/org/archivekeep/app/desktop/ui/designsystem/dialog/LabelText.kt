package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@Composable
fun LabelText(text: String) {
    Text(text, fontSize = 11.sp)
}
