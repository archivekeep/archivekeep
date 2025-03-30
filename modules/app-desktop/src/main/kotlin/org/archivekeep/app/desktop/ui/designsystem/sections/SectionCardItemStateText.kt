package org.archivekeep.app.desktop.ui.designsystem.sections

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.utils.loading.Loadable

@Composable
fun SectionCardItemStateText(loadable: Loadable<String>) {
    when (loadable) {
        is Loadable.Failed ->
            Text(
                "ERROR: ${loadable.throwable.message}",
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

@Composable
fun SectionCardItemStateText(loadable: OptionalLoadable<String>) {
    when (loadable) {
        is OptionalLoadable.Failed ->
            Text(
                "ERROR: ${loadable.cause.message}",
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )

        is OptionalLoadable.Loading ->
            Text(
                "Loading ...",
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )

        is OptionalLoadable.NotAvailable ->
            Text(
                "Not available ...",
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )

        is OptionalLoadable.LoadedAvailable ->
            Text(
                text = loadable.value,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
    }
}
