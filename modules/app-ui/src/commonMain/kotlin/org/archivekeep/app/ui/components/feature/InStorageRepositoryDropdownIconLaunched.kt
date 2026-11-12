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
import compose.icons.tablericons.InfoCircle
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dropdownmenu.ActionDropdownMenuItem
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItemIconButton
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_details
import org.archivekeep.ui.resources.common_forget
import org.archivekeep.ui.resources.common_more_actions
import org.jetbrains.compose.resources.stringResource

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
            contentDescription = stringResource(Res.string.common_more_actions),
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
                    stringResource(Res.string.common_forget),
                    isPending = false,
                    onLaunch = {
                        launchers.openForgetRepository(uri)
                    },
                ),
                ::closeDropdown,
            )
            ActionDropdownMenuItem(
                Action(
                    icon = TablerIcons.InfoCircle,
                    title = stringResource(Res.string.common_details),
                    isPending = false,
                    onLaunch = {
                        launchers.openRepositoryDetails(uri)
                    },
                ),
                ::closeDropdown,
            )
        }
    }
}
