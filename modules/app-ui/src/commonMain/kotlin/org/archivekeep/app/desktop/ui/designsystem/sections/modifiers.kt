package org.archivekeep.app.desktop.ui.designsystem.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

fun Modifier.sectionCardItem() =
    this
        .fillMaxWidth()
        .padding(
            start = sectionCardHorizontalPadding,
            end = sectionCardHorizontalPadding,
        )
