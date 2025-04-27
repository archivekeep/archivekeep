package org.archivekeep.app.ui.components.designsystem.layout.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme

@Composable
fun ViewLoading() {
    val viewPadding = AppTheme.dimens.viewPadding

    Box(
        Modifier
            .padding(
                PaddingValues(
                    start = viewPadding,
                    end = viewPadding,
                    top = viewPadding,
                    bottom = viewPadding,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
