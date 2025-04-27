package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.core.utils.generics.OptionalLoadable

@Composable
fun SectionCardBottomListItem(
    title: String,
    statusText: OptionalLoadable<String>,
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp,
                    horizontal = sectionCardHorizontalPadding,
                ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.Black.copy(alpha = 0.75f),
        ) {
            Box(
                Modifier.padding(start = 2.dp, end = 8.dp).size(16.dp),
                contentAlignment = Alignment.Center,
                content = icon,
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                title,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 14.sp,
                lineHeight = 16.sp,
            )

            statusText.let { SectionCardItemStateText(it) }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
