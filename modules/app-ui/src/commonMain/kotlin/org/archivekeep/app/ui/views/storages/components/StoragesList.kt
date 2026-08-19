package org.archivekeep.app.ui.views.storages.components

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.sections.EmptySectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardGrid
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.views.storages.StoragesViewState
import org.archivekeep.utils.loading.Loadable

@Composable
fun StoragesList(
    allStoragesLoadable: Loadable<List<StoragesViewState.Storage>>,
    emptyText: String,
) {
    LoadableGuard(allStoragesLoadable) { allStorages ->
        SectionCardGrid {
            if (allStorages.isEmpty()) {
                EmptySectionCard(emptyText)
            }

            allStorages.forEach { StorageEntry(it) }
        }
    }
}
