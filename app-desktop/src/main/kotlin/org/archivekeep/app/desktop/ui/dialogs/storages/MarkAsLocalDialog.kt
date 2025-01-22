package org.archivekeep.app.desktop.ui.dialogs.storages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.desktop.domain.wiring.LocalRegistry
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton

class MarkAsLocalDialog(
    uri: StorageURI,
) : AbstractStorageDialog<MarkAsLocalDialog.VM>(uri) {
    class VM(
        val storage: Storage,
    ) : IVM {
        override val title = "Mark as local"
    }

    @Composable
    override fun createVM(storage: Storage): VM = remember { VM(storage) }

    @Composable
    override fun renderContent(vm: VM) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text =
                buildAnnotatedString {
                    append("Mark storage ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(vm.storage.label)
                    }
                    append(" as local")
                },
        )
    }

    @Composable
    override fun RowScope.renderButtons(
        onClose: () -> Unit,
        vm: VM,
    ) {
        val registry = LocalRegistry.current
        val coroutineScope = rememberCoroutineScope()
        var runningJob by remember {
            mutableStateOf<Job?>(null)
        }

        DialogPrimaryButton(
            "Mark as local",
            onClick = {
                runningJob =
                    coroutineScope.launch {
                        registry.updateStorage(
                            uri,
                        ) {
                            it.copy(
                                isLocal = true,
                            )
                        }
                        onClose()
                    }
            },
        )
        Spacer(Modifier.weight(1f))
        DialogDismissButton("Cancel", onClick = onClose)
    }
}
