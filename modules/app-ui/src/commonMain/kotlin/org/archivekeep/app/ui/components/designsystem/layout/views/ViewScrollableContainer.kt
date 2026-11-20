package org.archivekeep.app.ui.components.designsystem.layout.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme

@Composable
fun Modifier.viewHorizontalPadding(): Modifier {
    val dimens = AppTheme.dimens
    return this
        .consumeWindowInsets(
            PaddingValues(
                start = dimens.viewPadding,
                end = dimens.viewPadding + dimens.viewExtraPaddingForScrollbar,
            ),
        ).windowInsetsPadding(WindowInsets.mandatorySystemGestures.union(WindowInsets.safeDrawing).only(WindowInsetsSides.Horizontal))
        .padding(
            PaddingValues(
                start = dimens.viewPadding,
                end = dimens.viewPadding + dimens.viewExtraPaddingForScrollbar,
            ),
        )
}

@Composable
fun Modifier.viewVerticalPadding(): Modifier {
    val dimens = AppTheme.dimens
    return this
        .consumeWindowInsets(
            PaddingValues(
                top = dimens.viewPadding / 2,
                end = dimens.viewPadding + dimens.viewExtraPaddingForScrollbar,
            ),
        ).windowInsetsPadding(WindowInsets.mandatorySystemGestures.union(WindowInsets.safeDrawing).only(WindowInsetsSides.Vertical))
        .padding(
            PaddingValues(
                top = dimens.viewPadding,
                bottom = dimens.viewPadding,
            ),
        )
}

@Composable
fun ViewScrollableContainer(
    columnModifier: Modifier = Modifier.viewHorizontalPadding().viewVerticalPadding(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = AppTheme.dimens

    ScrollableColumn(
        columnModifier = columnModifier,
        verticalArrangement = Arrangement.spacedBy(dimens.viewItemSpacing),
        scrollbarPadding = ScrollbarPadding,
        content = content,
    )
}
