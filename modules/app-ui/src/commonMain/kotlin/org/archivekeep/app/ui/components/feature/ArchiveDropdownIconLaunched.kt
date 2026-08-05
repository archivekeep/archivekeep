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
import org.archivekeep.app.ui.components.designsystem.dropdownmenu.ActionDropdownMenuItem
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitleIconButton
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveUiActions

@Composable
fun ArchiveDropdownIconLaunched(actions: HomeLocalArchiveUiActions) {
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

        SectionCardTitleIconButton(
            icon = TablerIcons.DotsVertical,
            onClick = {
                isDropdownExpanded = !isDropdownExpanded
            },
        )
        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false },
        ) {
            with(actions) {
                ActionDropdownMenuItem(unlock, ::closeDropdown)
                ActionDropdownMenuItem(add, ::closeDropdown)
                ActionDropdownMenuItem(cleanupFiles, ::closeDropdown)
                ActionDropdownMenuItem(reindex, ::closeDropdown)
                ActionDropdownMenuItem(associate, ::closeDropdown)
                ActionDropdownMenuItem(unassociate, ::closeDropdown)
                ActionDropdownMenuItem(forget, ::closeDropdown)
                ActionDropdownMenuItem(deinitialize, ::closeDropdown)
            }
        }
    }
}
