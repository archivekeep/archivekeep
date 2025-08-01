package org.archivekeep.app.ui.components.feature

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import compose.icons.TablerIcons
import compose.icons.tablericons.DotsVertical
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.storages.needsUnlock
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItemIconButton
import org.archivekeep.app.ui.components.feature.repository.WithRepositoryOpener
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers

@Composable
fun InArchiveRepositoryDropdownIconLaunched(
    repository: Repository,
    isAssociated: Boolean,
    canAdd: Boolean = false,
) {
    val repositoryURI = repository.uri
    val operationsLaunchers = LocalArchiveOperationLaunchers.current

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
            WithRepositoryOpener(repositoryURI) {
                DropdownMenuItem(onClick = {
                    openRepository()
                    closeDropdown()
                }, text = {
                    Text("Open")
                })
            }

            if (canAdd) {
                DropdownMenuItem(onClick = {
                    operationsLaunchers.openIndexUpdateOperation(repositoryURI)
                    closeDropdown()
                }, text = {
                    Text("Add")
                })
            }

            if (repository.optionalAccessorFlow
                    .map { it.needsUnlock() }
                    .collectAsState(false)
                    .value
            ) {
                DropdownMenuItem(onClick = {
                    operationsLaunchers.unlockRepository(repositoryURI, null)
                    closeDropdown()
                }, text = {
                    Text("Unlock")
                })
            }

            if (isAssociated) {
                DropdownMenuItem(onClick = {
                    operationsLaunchers.openUnassociateRepository(repositoryURI)
                    closeDropdown()
                }, text = {
                    Text("Unassociate")
                })
            } else {
                DropdownMenuItem(onClick = {
                    operationsLaunchers.openAssociateRepository(repositoryURI)
                    closeDropdown()
                }, text = {
                    Text("Associate")
                })
            }

            DropdownMenuItem(onClick = {
                operationsLaunchers.openForgetRepository(repositoryURI)
                closeDropdown()
            }, text = {
                Text("Forget")
            })
        }
    }
}
