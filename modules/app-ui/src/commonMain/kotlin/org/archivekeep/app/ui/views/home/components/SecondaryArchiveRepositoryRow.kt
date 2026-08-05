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
import compose.icons.tablericons.Lock
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItem
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItemIconActionButton
import org.archivekeep.app.ui.components.feature.InArchiveRepositoryDropdownIconLaunched
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.app.ui.views.home.model.RepositoryItemUiState
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

@Composable
fun SecondaryArchiveRepositoryRow(
    nonPrimaryRepository: RepositoryItemUiState,
    icon: ImageVector,
    name: String = nonPrimaryRepository.repo.reference.displayName,
) {
    val launchers = LocalArchiveOperationLaunchers.current

    val actions = nonPrimaryRepository.actions(launchers)

    val iconActionsList =
        listOf(
            actions.unlock,
            actions.add,
            actions.cleanupFiles,
            actions.reindex,
            actions.push,
            actions.pull,
        ).filterIsInstance<Loadable.Loaded<Action>>()
            .map { it.value }
            .filter { it.isAvailable }

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
            } else if (nonPrimaryRepository.repositoryOperationalState.needsUnlock.mapIfLoadedOrDefault(false) { it }) {
                Icon(TablerIcons.Lock, "Locked")
            } else {
                Icon(icon, "Storage")
            }
        },
        actions = {
            iconActionsList.forEach { SectionCardBottomListItemIconActionButton(it) }
            InArchiveRepositoryDropdownIconLaunched(listOf(actions.push, actions.pull), actions)
        },
    )
}
