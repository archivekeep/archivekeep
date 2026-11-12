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
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardHorizontalPadding
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_error
import org.archivekeep.ui.resources.common_loading_dots
import org.archivekeep.ui.resources.common_locked
import org.archivekeep.ui.resources.home_card_status_unavailable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.jetbrains.compose.resources.stringResource

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
                is NeedsUnlock -> stringResource(Res.string.common_locked)
                is OptionalLoadable.NotAvailable -> stringResource(Res.string.home_card_status_unavailable, loadable.cause?.message ?: loadable.javaClass.name)
                is OptionalLoadable.Failed -> stringResource(Res.string.common_error, loadable.cause.message ?: "Unknown error")
                is OptionalLoadable.Loading -> stringResource(Res.string.common_loading_dots)
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
