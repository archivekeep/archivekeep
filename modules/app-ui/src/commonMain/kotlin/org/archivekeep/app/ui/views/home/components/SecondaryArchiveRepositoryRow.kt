package org.archivekeep.app.ui.views.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.views.home.components.util.itemActions
import org.archivekeep.app.ui.views.home.model.RepositoryItemUiState

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
        connectionStatus = nonPrimaryRepository.connectionStatus,
        name = name,
        icon = icon,
        iconActions = actions.itemActions(),
        secondaryRepositoryActions = listOf(actions.push, actions.pull),
        repositoryActions = actions,
    )
}
