package org.archivekeep.app.ui.dialogs.other

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.ui.components.designsystem.dialog.DialogOverlay
import org.archivekeep.app.ui.dialogs.Dialog
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_close
import org.archivekeep.ui.resources.dialog_unsupported_message
import org.archivekeep.ui.resources.dialog_unsupported_title
import org.jetbrains.compose.resources.stringResource

class UnsupportedFeatureDialog : Dialog {
    @Composable
    override fun render(onClose: () -> Unit) {
        DialogOverlay(onDismissRequest = onClose) {
            DialogCardWithDialogInnerContainer(
                title = buildAnnotatedString { append(stringResource(Res.string.dialog_unsupported_title)) },
                content = {
                    Text(stringResource(Res.string.dialog_unsupported_message))
                },
                bottomContent = {
                    DialogButtonContainer {
                        DialogDismissButton(
                            stringResource(Res.string.common_close),
                            onClick = onClose,
                        )
                    }
                },
            )
        }
    }
}
