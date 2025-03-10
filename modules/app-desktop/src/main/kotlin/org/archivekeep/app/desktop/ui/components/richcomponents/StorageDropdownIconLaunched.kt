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
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.desktop.domain.wiring.LocalStorageOperationsLaunchers
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.designsystem.sections.SectionCardTitleIconButton
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.utils.loading.mapLoadedData

@Composable
fun StorageDropdownIconLaunched(uri: StorageURI) {
    val storageService = LocalStorageService.current
    val operationsLaunchers = LocalStorageOperationsLaunchers.current

    val storageLoadable =
        remember(storageService, uri) {
            storageService
                .allStorages
                .mapLoadedData { allStorages ->
                    allStorages.firstOrNull {
                        it.uri == uri
                    }
                }
        }.collectLoadableFlow()

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
            LoadableGuard(storageLoadable) { storage ->
                if (storage == null) {
                    Text("ERROR")
                    return@LoadableGuard
                }

                DropdownMenuItem(onClick = {
                    operationsLaunchers.openRename(uri)
                    isDropdownExpanded = false
                }, text = { Text(if (storage.knownStorage.registeredStorage?.label != null) "Rename" else "Set name") })

                if (!storage.isLocal) {
                    DropdownMenuItem(onClick = {
                        operationsLaunchers.openMarkAsLocal(uri)
                        isDropdownExpanded = false
                    }, text = { Text("Mark as local") })
                }
                if (storage.isLocal) {
                    DropdownMenuItem(onClick = {
                        operationsLaunchers.openMarkAsExternal(uri)
                        isDropdownExpanded = false
                    }, text = { Text("Mark as external") })
                }
            }
        }
    }
}
