package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialogButtonContainer(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.padding(top = DialogContentButtonsSpacing).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun (RowScope).DialogPrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.alignByBaseline(),
    ) {
        Text(text)
    }
}

@Composable
fun (RowScope).DialogSecondaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = ButtonDefaults.outlinedButtonBorder(enabled),
        colors = ButtonDefaults.outlinedButtonColors(),
        modifier = Modifier.alignByBaseline(),
    ) {
        Text(text)
    }
}

@Composable
fun (RowScope).DialogDismissButton(
    text: String = "Cancel",
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        enabled = enabled,
        modifier = Modifier.alignByBaseline(),
    ) {
        Text(text)
    }
}

@Composable
fun (RowScope).DialogButtonsStatusText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier.alignByBaseline(),
        fontSize = 14.sp,
        color = Color.Black.copy(alpha = 0.8f),
    )
}
