package org.archivekeep.app.ui.views.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.model.RepositoryItemUiState
import org.archivekeep.utils.loading.mapIfLoadedOrDefault

@Composable
fun SecondaryArchiveRepositoryRow(
    nonPrimaryRepository: RepositoryItemUiState,
    icon: ImageVector,
    name: String = nonPrimaryRepository.repo.reference.displayName,
) {
    val launchers = LocalArchiveOperationLaunchers.current

    val actions = nonPrimaryRepository.actions(launchers)

    RepositoryRow(
        statusText = nonPrimaryRepository.texts,
        isLoading = nonPrimaryRepository.isLoading,
        isConnected = nonPrimaryRepository.connectionStatus.isConnected,
        needsUnlock = nonPrimaryRepository.repositoryOperationalState.needsUnlock.mapIfLoadedOrDefault(false) { it },
        name = name,
        icon = icon,
        iconActions = actions.iconActions(),
        secondaryRepositoryActions = listOf(actions.push, actions.pull),
        repositoryActions = actions,
    )
}
