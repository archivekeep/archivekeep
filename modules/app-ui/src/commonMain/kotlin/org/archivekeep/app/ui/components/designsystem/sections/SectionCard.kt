package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.layout.views.SectionCardShape
import org.archivekeep.app.ui.components.designsystem.theme.CColors

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (cardColors, border) =
        MaterialTheme.colorScheme.let {
            remember {
                Pair(
                    CardColors(
                        containerColor = Color.White,
                        contentColor = Color.DarkGray,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.LightGray,
                    ),
                    BorderStroke(
                        width = 1.dp,
                        color = CColors.cardsGridCardBorder,
                    ),
                )
            }
        }

    OutlinedCard(
        modifier = modifier,
        border = border,
        shape = SectionCardShape,
        colors = cardColors,
    ) {
        content()
    }
}
