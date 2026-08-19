package org.archivekeep.app.ui.components.designsystem.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ErrorAlert(content: @Composable ColumnScope.() -> Unit) {
    Alert(Color.Red, Color.Red.copy(alpha = 0.1f), content)
}

@Composable
fun WarningAlert(content: @Composable ColumnScope.() -> Unit) {
    Alert(Color.Yellow, Color.Yellow.copy(alpha = 0.1f), content)
}

@Composable
fun SuccessAlert(content: @Composable ColumnScope.() -> Unit) {
    Alert(Color.Green, Color.Green.copy(alpha = 0.1f), content)
}

@Composable
private fun Alert(
    borderColor: Color,
    surfaceColor: Color,
    content: @Composable (ColumnScope.() -> Unit),
) {
    Surface(
        border =
            BorderStroke(
                width = 1.dp,
                color = borderColor,
            ),
        color = surfaceColor,
        shape = MaterialTheme.shapes.medium,
    ) {
        SelectionContainer {
            Column(
                Modifier.padding(12.dp),
                content = content,
            )
        }
    }
}
