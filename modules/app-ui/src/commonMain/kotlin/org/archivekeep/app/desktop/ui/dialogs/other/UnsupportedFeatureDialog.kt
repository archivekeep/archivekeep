package org.archivekeep.app.desktop.ui.dialogs.other

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlay
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.jetbrains.compose.ui.tooling.preview.Preview

class UnsupportedFeatureDialog : Dialog {
    @Composable
    fun renderDialogCard(onClose: () -> Unit) {
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

    @Composable
    override fun render(onClose: () -> Unit) {
        DialogOverlay(onDismissRequest = onClose) {
            renderDialogCard(onClose)
        }
    }
}

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        val dialog = UnsupportedFeatureDialog()

        dialog.renderDialogCard(
            onClose = {},
        )
    }
}
