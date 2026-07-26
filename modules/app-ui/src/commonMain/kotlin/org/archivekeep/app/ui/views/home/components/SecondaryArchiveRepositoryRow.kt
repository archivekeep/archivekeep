package org.archivekeep.app.ui.views.home.components

import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.Download
import compose.icons.tablericons.Lock
import compose.icons.tablericons.Plus
import compose.icons.tablericons.Trash
import compose.icons.tablericons.Upload
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItem
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItemIconActionButton
import org.archivekeep.app.ui.components.feature.InArchiveRepositoryDropdownIconLaunched
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.Action2
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveSecondaryRepositoryUiState

@Composable
fun SecondaryArchiveRepositoryRow(
    nonPrimaryRepository: HomeLocalArchiveSecondaryRepositoryUiState,
    icon: ImageVector,
    name: String = nonPrimaryRepository.repo.reference.displayName,
) {
    val storageRepo = nonPrimaryRepository.repo
    val repository = storageRepo.repository
    val launchers = LocalArchiveOperationLaunchers.current

    val actionAdd =
        Action2(
            TablerIcons.Plus,
            "Add new files",
            enabled = nonPrimaryRepository.canAdd,
            onClick = {
                launchers.openIndexUpdateOperation(repository.uri)
            },
        )

    val actionPush =
        Action2(
            TablerIcons.Upload,
            "Push",
            enabled = nonPrimaryRepository.canPush,
            onClick = {
                launchers.pushToRepo(
                    repository.uri,
                    storageRepo.primaryRepositoryURI!!,
                )
            },
        )

    val actionPull =
        Action2(
            TablerIcons.Download,
            "Pull",
            enabled = nonPrimaryRepository.canPull,
            onClick = {
                launchers.pullFromRepo(
                    repository.uri,
                    storageRepo.primaryRepositoryURI!!,
                )
            },
        )

    val cleanupFiles =
        if (nonPrimaryRepository.canCleanupDeletedFiles) {
            Action2(
                TablerIcons.Trash,
                "Cleanup deleted files",
                enabled = true,
                onClick = {
                    launchers.openDeletedFilesCleanupOperation(repository.uri)
                },
            )
        } else {
            null
        }

    val actions = listOfNotNull(actionAdd, actionPush, actionPull, cleanupFiles)

    SectionCardBottomListItem(
        title = name,
        statusText = nonPrimaryRepository.texts,
        modifier = if (!nonPrimaryRepository.connectionStatus.isConnected) Modifier.alpha(0.6f) else Modifier,
        icon = {
            if (nonPrimaryRepository.isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current.let { it.copy(alpha = it.alpha * 0.7f) },
                )
            } else if (nonPrimaryRepository.needsUnlock) {
                Icon(TablerIcons.Lock, "Locked")
            } else {
                Icon(icon, "Storage")
            }
        },
        actions = {
            actions.forEach { SectionCardBottomListItemIconActionButton(it) }
            InArchiveRepositoryDropdownIconLaunched(
                repository = repository,
                actions = actions,
                canReindex = nonPrimaryRepository.canReindex,
                isAssociated = storageRepo.otherRepositoryState.associationId != null,
            )
        },
    )
}
