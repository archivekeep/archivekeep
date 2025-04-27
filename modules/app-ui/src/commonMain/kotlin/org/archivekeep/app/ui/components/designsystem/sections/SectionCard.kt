package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.layout.views.SectionCardShape
import org.archivekeep.app.ui.components.designsystem.theme.CColors

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val (folderButtonColors, cardColors, folderButtonBorder) =
        MaterialTheme.colorScheme.let {
            remember {
                Triple(
                    ButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.DarkGray,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.LightGray,
                    ),
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

    Card(
        colors = cardColors,
        shape = SectionCardShape,
        modifier =
            modifier
                .shadow(
                    4.dp,
                    shape = SectionCardShape,
                    ambientColor = Color.Blue,
                    spotColor = CColors.cardsGridCardBorder,
                ).let {
                    if (!enabled) {
                        modifier
                    } else {
                        modifier
                    }
                },
        elevation =
            CardDefaults.cardElevation(
                3.dp,
            ),
    ) {
        content()
    }
}
