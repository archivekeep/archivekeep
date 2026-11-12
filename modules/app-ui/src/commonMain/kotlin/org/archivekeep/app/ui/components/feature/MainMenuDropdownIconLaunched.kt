package org.archivekeep.app.ui.components.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import compose.icons.TablerIcons
import compose.icons.tablericons.DotsVertical
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_menu
import org.archivekeep.ui.resources.main_menu_add_file_repository
import org.archivekeep.ui.resources.main_menu_add_remote_repository
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainMenuDropdownIconLaunched(modifier: Modifier = Modifier) {
    val operationsLaunchers = LocalArchiveOperationLaunchers.current

    var isDropdownExpanded by
        remember {
            mutableStateOf(false)
        }

    Surface(
        modifier = modifier.semantics { role = Role.DropdownList },
        onClick = { isDropdownExpanded = true },
        contentColor = Color(200, 210, 240),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
    ) {
        Icon(
            TablerIcons.DotsVertical,
            tint = Color(200, 210, 240),
            contentDescription = stringResource(Res.string.common_menu),
            modifier =
                Modifier
                    .padding(6.dp)
                    .size(24.dp),
        )

        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false },
        ) {
            DropdownMenuItem(onClick = {
                operationsLaunchers.openAddFileSystemRepository()
                isDropdownExpanded = false
            }, text = {
                Text(stringResource(Res.string.main_menu_add_file_repository))
            })
            DropdownMenuItem(onClick = {
                operationsLaunchers.openAddRemoteRepository()
                isDropdownExpanded = false
            }, text = {
                Text(stringResource(Res.string.main_menu_add_remote_repository))
            })
        }
    }
}
