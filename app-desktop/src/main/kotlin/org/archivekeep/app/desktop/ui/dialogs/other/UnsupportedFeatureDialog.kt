package org.archivekeep.app.desktop.ui.dialogs.other

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.buildAnnotatedString
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlay
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.Dialog

class UnsupportedFeatureDialog : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        DialogOverlay(onDismissRequest = onClose) {
            DialogCardWithDialogInnerContainer(
                title = buildAnnotatedString { append("Feature not implemented") },
                content = {
                    Text("This feature is not implemented")
                },
                bottomContent = {
                    DialogButtonContainer {
                        DialogPrimaryButton(
                            "Closw",
                            onClick = onClose,
                        )
                    }
                },
            )
        }
    }
}
