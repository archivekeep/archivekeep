package org.archivekeep.app.ui.components.designsystem.layout.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme

@Composable
fun ViewScrollableContainer(content: @Composable ColumnScope.() -> Unit) {
    val dimens = AppTheme.dimens

    ScrollableColumn(
        columnModifier =
            Modifier
                .consumeWindowInsets(
                    PaddingValues(
                        top = dimens.viewPadding / 2,
                        start = dimens.viewPadding,
                        end = dimens.viewPadding + dimens.viewExtraPaddingForScrollbar,
                        bottom = dimens.viewPadding,
                    ),
                ).windowInsetsPadding(WindowInsets.mandatorySystemGestures.union(WindowInsets.safeDrawing))
                .padding(
                    PaddingValues(
                        start = dimens.viewPadding,
                        end = dimens.viewPadding + dimens.viewExtraPaddingForScrollbar,
                        top = dimens.viewPadding,
                        bottom = dimens.viewPadding,
                    ),
                ),
        verticalArrangement = Arrangement.spacedBy(dimens.viewItemSpacing),
        scrollbarPadding = ScrollbarPadding,
        content = content,
    )
}
