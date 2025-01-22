package org.archivekeep.app.desktop.ui.designsystem.sections

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionCardButton(
    onClick: () -> Unit,
    text: String,
    running: Boolean = false,
) {
    Button(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraSmall,
        contentPadding = PaddingValues(12.dp, 4.dp),
        modifier = Modifier.defaultMinSize(1.dp, 32.dp),
    ) {
        if (running) {
            CircularProgressIndicator(
                Modifier
                    .padding(end = 8.dp)
                    .then(Modifier.size(12.dp)),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }

        Text(text, fontSize = 13.sp)
    }
}
