package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DialogFilePicker(
    path: String?,
    onTriggerChange: () -> Unit,
    changeEnabled: Boolean = true,
) {
    val rounding = 9.dp

    Row(
        modifier =
            Modifier
                .padding(vertical = 4.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
    ) {
        Surface(
            Modifier.fillMaxHeight().weight(1f),
            shape = RoundedCornerShape(topStart = rounding, bottomStart = rounding),
        ) {
            Column(
                Modifier.padding(vertical = 4.dp, horizontal = 12.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                path.let {
                    if (it == null) {
                        Text(
                            "No directory selected",
                            color = Color.Black.copy(alpha = 0.4f),
                            softWrap = true,
                        )
                    } else {
                        Text(it, softWrap = true)
                    }
                }
            }
        }

        Button(
            onClick = onTriggerChange,
            enabled = changeEnabled,
            modifier = Modifier.fillMaxHeight(),
            shape = RoundedCornerShape(topEnd = rounding, bottomEnd = rounding),
        ) {
            Text(if (path != null) "Change" else "Select")
        }
    }
}
