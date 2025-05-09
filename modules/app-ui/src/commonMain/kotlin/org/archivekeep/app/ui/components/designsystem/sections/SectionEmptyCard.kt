package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.layout.views.SectionCardShape

@Composable
fun SectionEmptyCard(content: @Composable ColumnScope.() -> Unit) {
    val cardColors =
        remember {
            CardColors(
                containerColor = Color(0xFFFCFCFC),
                contentColor = Color(0xFFABABAB),
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.LightGray,
            )
        }

    Card(
        colors = cardColors,
        shape = SectionCardShape,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}
