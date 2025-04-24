package org.archivekeep.app.desktop.ui.views.home.components

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
import compose.icons.tablericons.Upload
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.ui.components.richcomponents.InArchiveRepositoryDropdownIconLaunched
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomListItem
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardBottomListItemIconButton
import org.archivekeep.app.desktop.ui.views.home.SecondaryArchiveRepository

@Composable
fun SecondaryArchiveRepositoryRow(
    nonPrimaryRepository: SecondaryArchiveRepository.State,
    icon: ImageVector,
    name: String = nonPrimaryRepository.repo.reference.displayName,
) {
    val storageRepo = nonPrimaryRepository.repo
    val repository = storageRepo.repository
    val launchers = LocalArchiveOperationLaunchers.current

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
            SectionCardBottomListItemIconButton(
                TablerIcons.Upload,
                contentDescription = "Push",
                enabled = nonPrimaryRepository.canPush,
                onClick = {
                    launchers.pushToRepo(
                        storageRepo.repository.uri,
                        storageRepo.primaryRepositoryURI!!,
                    )
                },
            )
            SectionCardBottomListItemIconButton(
                TablerIcons.Download,
                contentDescription = "Pull",
                enabled = nonPrimaryRepository.canPull,
                onClick = {
                    launchers.pullFromRepo(
                        storageRepo.repository.uri,
                        storageRepo.primaryRepositoryURI!!,
                    )
                },
            )
            InArchiveRepositoryDropdownIconLaunched(
                repository = repository,
                canAdd = nonPrimaryRepository.canAdd,
                isAssociated = storageRepo.otherRepositoryState.associationId != null,
            )
        },
    )
}
