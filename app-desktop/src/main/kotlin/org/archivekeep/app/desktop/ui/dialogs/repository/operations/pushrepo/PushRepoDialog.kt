package org.archivekeep.app.desktop.ui.dialogs.repository.operations.pushrepo

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.RelocationSyncModeOptions
import org.archivekeep.app.desktop.ui.components.SplitRow
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardItemStateText
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.app.desktop.utils.collectLoadableFlow

class PushRepoDialog(
    val repositoryURI: RepositoryURI,
) : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val scope = rememberCoroutineScope()

        val storageService = LocalStorageService.current
        val syncService = LocalRepoToRepoSyncService.current

        val vm =
            remember {
                PushRepoDialogViewModel(
                    scope,
                    repositoryURI,
                    storageService,
                    syncService,
                )
            }

        DialogOverlayCard(onDismissRequest = onClose) {
            LoadableGuard(
                vm.repoName.collectAsLoadable(),
                vm.otherRepos.collectAsLoadable(),
            ) { repoName, otherRepositoryCandidates ->
                DialogInnerContainer(
                    rememberDialogTitle(repoName),
                    content = {
                        RelocationSyncModeOptions(
                            vm.relocationSyncModeFlow.collectAsState().value,
                            onRelocationSyncModeChange = {
                                vm.relocationSyncModeFlow.value = it
                            },
                        )

                        Spacer(Modifier.height(4.dp))

                        LabelText("Repositories to sync:")

                        otherRepositoryCandidates.forEach {
                            SplitRow(
                                leftContent = {
                                    Text(it.otherRepository.storage.displayName)
                                    SectionCardItemStateText(it.statusText.collectLoadableFlow())
                                },
                                rightContent = {
                                    Text("Start")
                                },
                            )
                        }
                    },
                    bottomContent = {
                        DialogButtonContainer {
                            DialogPrimaryButton(
                                "Start all",
                                onClick = vm::startAllSync,
                                enabled = true,
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            DialogDismissButton(
                                "Dismiss",
                                onClick = onClose,
                                enabled = true,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun rememberDialogTitle(displayName: String) =
    remember(displayName) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(displayName)
                append(": ")
            }
            append("push")
        }
    }
