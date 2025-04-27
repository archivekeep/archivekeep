package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme

@Composable
fun DialogOverlay(
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties(),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(AppTheme.dimens.dialogContainerPadding)
                    .padding(bottom = dialogBottomPaddingHack()),
            content = content,
        )
    }
}

expect fun dialogProperties(): DialogProperties

expect fun dialogBottomPaddingHack(): Dp
