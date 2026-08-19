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
import org.archivekeep.app.core.domain.repositories.RepositoryConnectionState
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItem
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItemIconAvailableActions
import org.archivekeep.app.ui.components.feature.InArchiveRepositoryDropdownIconLaunched
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.app.ui.views.home.model.RepositoryBaseUiActions
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable

@Composable
fun RepositoryRow(
    statusText: OptionalLoadable<String>,
    isLoading: Boolean,
    connectionStatus: RepositoryConnectionState,
    icon: ImageVector,
    name: String,
    iconActions: List<Loadable<Action>>,
    secondaryRepositoryActions: List<Loadable<Action>>,
    repositoryActions: RepositoryBaseUiActions,
) {
    SectionCardBottomListItem(
        title = name,
        statusText = statusText,
        modifier = if (!connectionStatus.isAccessible) Modifier.alpha(0.6f) else Modifier,
        icon = {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current.let { it.copy(alpha = it.alpha * 0.7f) },
                )
            } else if (connectionStatus.isLocked) {
                Icon(TablerIcons.Lock, "Locked")
            } else {
                Icon(icon, "Storage")
            }
        },
        actions = {
            SectionCardBottomListItemIconAvailableActions(iconActions)
            InArchiveRepositoryDropdownIconLaunched(secondaryRepositoryActions, repositoryActions)
        },
    )
}
