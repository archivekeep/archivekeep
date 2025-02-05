package org.archivekeep.app.desktop.ui.views.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.desktop.ui.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.utils.Loadable

@Composable
fun HomeCardStateText(loadable: Loadable<String>) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 0.dp,
                    start = sectionCardHorizontalPadding,
                    bottom = 4.dp,
                    end = sectionCardHorizontalPadding,
                ),
    ) {
        when (loadable) {
            is Loadable.Failed ->
                Text(
                    "Failed ...",
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )

            is Loadable.Loading ->
                Text(
                    "Loading ...",
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )

            is Loadable.Loaded ->
                Text(
                    text = loadable.value,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
        }
    }
}
