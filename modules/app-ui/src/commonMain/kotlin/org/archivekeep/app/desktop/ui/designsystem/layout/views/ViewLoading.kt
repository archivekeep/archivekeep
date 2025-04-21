package org.archivekeep.app.desktop.ui.designsystem.layout.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ViewLoading() {
    Box(
        Modifier
            .padding(
                PaddingValues(
                    start = ViewPadding,
                    end = ViewPadding,
                    top = ViewPadding,
                    bottom = ViewPadding,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
