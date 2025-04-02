package org.archivekeep.app.desktop.ui.components.operations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.layout.scrollable.ScrollableColumn

@Composable
fun ScrollableLogTextInDialog(
    text: String,
    modifier: Modifier =
        Modifier
            .heightIn(min = 40.dp, max = 140.dp)
            .height(140.dp)
            .fillMaxWidth(),
) {
    ScrollableLogText(text, modifier)
}

@Composable
fun ScrollableLogText(
    text: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(0.05f),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)) {
            LabelText("Log")
            Spacer(Modifier.height(4.dp))

            ScrollableColumn(Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(text.trimEnd(), Modifier.fillMaxWidth())
                }
            }
        }
    }
}
