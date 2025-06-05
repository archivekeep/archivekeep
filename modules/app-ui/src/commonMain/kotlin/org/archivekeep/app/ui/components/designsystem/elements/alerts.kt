package org.archivekeep.app.ui.components.designsystem.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ErrorAlert(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        border =
            BorderStroke(
                width = 1.dp,
                color = Color.Red,
            ),
        color = Color.Red.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            Modifier.padding(12.dp),
            content = content,
        )
    }
}

@Composable
fun WarningAlert(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        border =
            BorderStroke(
                width = 1.dp,
                color = Color.Yellow,
            ),
        color = Color.Yellow.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            Modifier.padding(12.dp),
            content = content,
        )
    }
}
