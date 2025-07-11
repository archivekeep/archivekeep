package org.archivekeep.app.ui.views.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardHorizontalPadding

@Composable
fun HomeCardStateText(loadable: OptionalLoadable<String>) {
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
        val statusText =
            when (loadable) {
                is NeedsUnlock -> "Locked"
                is OptionalLoadable.NotAvailable -> "Status unavailable: ${loadable.cause?.message ?: loadable.javaClass.name}"
                is OptionalLoadable.Failed -> "ERROR: ${loadable.cause.message}"
                is OptionalLoadable.Loading -> "Loading ..."
                is OptionalLoadable.LoadedAvailable -> loadable.value
            }
        Text(
            statusText,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
    }
}
