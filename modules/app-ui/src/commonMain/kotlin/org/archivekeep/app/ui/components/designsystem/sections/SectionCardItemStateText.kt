package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_error
import org.archivekeep.ui.resources.common_loading_dots
import org.archivekeep.ui.resources.common_not_available
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.jetbrains.compose.resources.stringResource

@Composable
fun SectionCardItemStateText(loadable: Loadable<String>) {
    when (loadable) {
        is Loadable.Failed -> {
            Text(
                stringResource(Res.string.common_error, loadable.throwable.message ?: ""),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        is Loadable.Loading -> {
            Text(
                stringResource(Res.string.common_loading_dots),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        is Loadable.Loaded -> {
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

@Composable
fun SectionCardItemStateText(loadable: OptionalLoadable<String>) {
    when (loadable) {
        is OptionalLoadable.Failed -> {
            Text(
                stringResource(Res.string.common_error, loadable.cause.message ?: ""),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        is OptionalLoadable.Loading -> {
            Text(
                stringResource(Res.string.common_loading_dots),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        is OptionalLoadable.NotAvailable -> {
            Text(
                stringResource(Res.string.common_not_available),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        is OptionalLoadable.LoadedAvailable -> {
            if (!loadable.value.isBlank()) {
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
}
