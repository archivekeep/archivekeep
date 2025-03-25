package org.archivekeep.app.desktop.ui.components.richcomponents

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import compose.icons.TablerIcons
import compose.icons.tablericons.DotsVertical
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitleIconButton

@Composable
fun ArchiveDropdownIconLaunched(
    repositoryURI: RepositoryURI,
    isAssociated: Boolean,
) {
    val operationsLaunchers = LocalArchiveOperationLaunchers.current

    Box(
        contentAlignment = Alignment.BottomEnd,
    ) {
        var isDropdownExpanded by
            remember {
                mutableStateOf(false)
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
            if (isAssociated) {
                DropdownMenuItem(onClick = {
                    operationsLaunchers.openUnassociateRepository(repositoryURI)
                    isDropdownExpanded = false
                }, text = {
                    Text("Unassociate")
                })
            } else {
                DropdownMenuItem(onClick = {
                    operationsLaunchers.openAssociateRepository(repositoryURI)
                    isDropdownExpanded = false
                }, text = {
                    Text("Associate")
                })
            }

            DropdownMenuItem(onClick = {
                operationsLaunchers.openForgetRepository(repositoryURI)
                isDropdownExpanded = false
            }, text = {
                Text("Forget")
            })
        }
    }
}
