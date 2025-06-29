package org.archivekeep.app.ui.dialogs.other

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlay
import org.archivekeep.app.ui.dialogs.Dialog

class UnsupportedFeatureDialog : Dialog {
    @Composable
    override fun render(onClose: () -> Unit) {
        DialogOverlay(onDismissRequest = onClose) {
            DialogCardWithDialogInnerContainer(
                title = buildAnnotatedString { append("Feature not implemented") },
                content = {
                    Text("This feature is not implemented")
                },
                bottomContent = {
                    DialogButtonContainer {
                        DialogDismissButton(
                            "Close",
                            onClick = onClose,
                        )
                    }
                },
            )
        }
    }
}
