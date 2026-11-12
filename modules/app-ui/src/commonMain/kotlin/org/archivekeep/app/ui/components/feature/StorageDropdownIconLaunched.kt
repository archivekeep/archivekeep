package org.archivekeep.app.ui.components.feature

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
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitleIconButton
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalStorageOperationsLaunchers
import org.archivekeep.app.ui.utils.collectLoadableFlow
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_error
import org.archivekeep.ui.resources.common_forget
import org.archivekeep.ui.resources.common_rename
import org.archivekeep.ui.resources.storage_dropdown_mark_as_external
import org.archivekeep.ui.resources.storage_dropdown_mark_as_local
import org.archivekeep.ui.resources.storage_dropdown_set_name
import org.archivekeep.utils.loading.mapLoadedData
import org.jetbrains.compose.resources.stringResource

@Composable
fun StorageDropdownIconLaunched(uri: StorageURI) {
    val storageService = LocalApplicationServices.current.storageService
    val operationsLaunchers = LocalStorageOperationsLaunchers.current

    val storageLoadable =
        remember(storageService, uri) {
            storageService
                .allStoragesPartiallyResolved
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
                    Text(stringResource(Res.string.common_error, "Storage not found"))
                    return@LoadableGuard
                }

                DropdownMenuItem(onClick = {
                    operationsLaunchers.openRename(uri)
                    isDropdownExpanded = false
                }, text = { Text(if (storage.knownStorage.registeredStorage?.label != null) stringResource(Res.string.common_rename) else stringResource(Res.string.storage_dropdown_set_name)) })

                if (!storage.isLocal) {
                    DropdownMenuItem(onClick = {
                        operationsLaunchers.openMarkAsLocal(uri)
                        isDropdownExpanded = false
                    }, text = { Text(stringResource(Res.string.storage_dropdown_mark_as_local)) })
                }
                if (storage.isLocal) {
                    DropdownMenuItem(onClick = {
                        operationsLaunchers.openMarkAsExternal(uri)
                        isDropdownExpanded = false
                    }, text = { Text(stringResource(Res.string.storage_dropdown_mark_as_external)) })
                }
                DropdownMenuItem(onClick = {
                    operationsLaunchers.openForget(uri)
                    isDropdownExpanded = false
                }, text = { Text(stringResource(Res.string.common_forget)) })
            }
        }
    }
}
