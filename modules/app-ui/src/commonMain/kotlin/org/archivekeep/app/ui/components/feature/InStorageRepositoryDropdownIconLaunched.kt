package org.archivekeep.app.ui.components.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import compose.icons.TablerIcons
import compose.icons.tablericons.DotsVertical
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dropdownmenu.ActionDropdownMenuItem
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItemIconButton
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.app.ui.views.home.model.RepositoryBaseUiActions
import org.archivekeep.utils.loading.Loadable

@Composable
fun InStorageRepositoryDropdownIconLaunched(uri: RepositoryURI) {
    val launchers = LocalArchiveOperationLaunchers.current

    Box(
        contentAlignment = Alignment.BottomEnd,
    ) {
        var isDropdownExpanded by
            remember {
                mutableStateOf(false)
            }

        fun closeDropdown() {
            isDropdownExpanded = false
        }

        SectionCardBottomListItemIconButton(
            icon = TablerIcons.DotsVertical,
            contentDescription = "More",
            onClick = {
                isDropdownExpanded = !isDropdownExpanded
            },
        )
        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = ::closeDropdown,
        ) {
            ActionDropdownMenuItem(
                Action(
                    null,
                    "Forget",
                    isPending = false,
                    onLaunch = {
                        launchers.openForgetRepository(uri)
                    },
                ),
                ::closeDropdown,
            )
        }
    }
}
