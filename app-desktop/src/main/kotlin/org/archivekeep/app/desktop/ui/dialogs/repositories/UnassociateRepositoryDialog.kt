package org.archivekeep.app.desktop.ui.dialogs.repositories

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveService
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.mapLoadedData

class UnassociateRepositoryDialog(
    val uri: RepositoryURI,
) : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val archiveService = LocalArchiveService.current
        val repositoryService = LocalRepoService.current

        val stateLoadable =
            remember(archiveService, uri) {
                archiveService
                    .allArchives
                    .mapLoadedData { allArchives ->
                        val currentArchive =
                            allArchives
                                .first { it.repositories.any { it.second.uri == uri } }

                        val currentRepo = currentArchive.repositories.first { it.second.uri == uri }

                        Pair(currentArchive, currentRepo)
                    }
            }.collectLoadableFlow()

        val repository = repositoryService.getRepository(uri)

        val coroutineScope = rememberCoroutineScope()
        var runningJob by remember {
            mutableStateOf<Job?>(null)
        }

        DialogOverlayCard(onDismissRequest = onClose) {
            LoadableGuard(
                stateLoadable,
            ) { (currentArchive, currentRepo) ->
                DialogInnerContainer(
                    buildAnnotatedString {
                        append("Unassociate repository")
                    },
                    content = {
                        Text(
                            remember(currentArchive, currentRepo) {
                                buildAnnotatedString {
                                    append("Unassociate repository ")

                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(currentRepo.second.displayName)
                                    }
                                    append(" in storage ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append(currentRepo.first.label)
                                    }
                                    append(".")
                                }
                            },
                        )
                    },
                    bottomContent = {
                        DialogButtonContainer {
                            DialogPrimaryButton(
                                "Unassociate",
                                onClick = {
                                    runningJob =
                                        coroutineScope.launch {
                                            repository.updateMetadata {
                                                it.copy(
                                                    associationGroupId = null,
                                                )
                                            }
                                            onClose()
                                        }
                                },
                                enabled = true,
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            DialogDismissButton(
                                "Cancel",
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
