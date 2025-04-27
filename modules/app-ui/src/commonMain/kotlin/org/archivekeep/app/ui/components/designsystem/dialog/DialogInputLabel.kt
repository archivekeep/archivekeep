package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DialogInputLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}
