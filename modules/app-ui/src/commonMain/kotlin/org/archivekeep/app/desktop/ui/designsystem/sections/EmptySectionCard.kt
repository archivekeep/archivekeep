package org.archivekeep.app.desktop.ui.designsystem.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.desktop.ui.designsystem.layout.views.SectionCardShape

@Composable
fun EmptySectionCard(text: String) {
    Surface(
        color = Color(0, 0, 0, 5),
        shape = SectionCardShape,
    ) {
        Text(
            text,
            color = Color(0, 0, 0, 160),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(12.dp),
        )
    }
}
