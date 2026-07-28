package org.archivekeep.app.ui.components.designsystem.elements

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val warningColor = Color(0xFFFFCF21)
val warningTextColor = Color.Black.copy(alpha = 0.8f).compositeOver(warningColor)

@Composable
fun WarningBadge(content: @Composable () -> Unit) {
    Surface(
        color = warningColor,
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        SelectionContainer(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            CompositionLocalProvider(
                LocalContentColor provides warningTextColor,
                LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 11.sp, lineHeight = 11.sp),
                content = content,
            )
        }
    }
}
