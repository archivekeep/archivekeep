package org.archivekeep.app.desktop.ui.designsystem.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionCardTitle(
    isLoading: Boolean,
    text: String,
    grayOutText: Boolean = false,
    subtitle: (@Composable ColumnScope.() -> Unit)? = null,
    icons: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 8.dp,
                    start = sectionCardHorizontalPadding,
                    end = sectionCardHorizontalPadding,
                    bottom = 2.dp,
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    Modifier
                        .padding(horizontal = sectionCardHorizontalPadding)
                        .then(Modifier.size(14.dp)),
                    color = Color.Gray,
                    strokeWidth = 2.dp,
                )
            }

            Column {
                Text(
                    text,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    textAlign = TextAlign.Left,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (grayOutText) Color.LightGray else Color.Unspecified,
                )

                if (subtitle != null) {
                    subtitle()
                }
            }
        }

        Row {
            icons()
        }
    }
}
