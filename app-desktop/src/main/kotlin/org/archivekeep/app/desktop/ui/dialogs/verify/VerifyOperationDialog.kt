package org.archivekeep.app.desktop.ui.dialogs.verify

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.ui.dialogs.Dialog

class VerifyOperationDialog(
    val repositoryURI: RepositoryURI,
) : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        Dialog(
            onDismissRequest = onClose,
        ) {
            Surface(
                color = Color.Red,
            ) {
                Box(Modifier.padding(12.dp)) {
                    Text("Verify Archive index: $repositoryURI")
                }
            }
        }
    }
}
