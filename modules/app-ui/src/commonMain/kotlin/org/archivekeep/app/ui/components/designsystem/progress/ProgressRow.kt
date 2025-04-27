package org.archivekeep.app.ui.components.designsystem.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ProgressRow(
    progress: () -> Float,
    text: String,
    subList: (@Composable () -> Unit)? = null,
) {
    if (subList != null) {
        Column {
            ProgressRow(progress, text)
            Box(
                Modifier.padding(start = 24.dp),
            ) {
                Column(
                    Modifier.padding(top = 8.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    subList()
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                progress = progress,
                strokeWidth = 2.dp,
                trackColor = Color.LightGray,
                gapSize = 0.dp,
                modifier =
                    Modifier.size(16.dp),
            )
            Text(text, overflow = TextOverflow.Ellipsis, maxLines = 1)
        }
    }
}
