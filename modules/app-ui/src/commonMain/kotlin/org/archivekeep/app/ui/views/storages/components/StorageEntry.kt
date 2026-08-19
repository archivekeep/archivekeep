package org.archivekeep.app.ui.views.storages.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.elements.ConnectionStatusTag
import org.archivekeep.app.ui.components.designsystem.sections.SectionCard
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomList
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardBottomListItem
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardTitle
import org.archivekeep.app.ui.components.designsystem.sections.sectionCardItem
import org.archivekeep.app.ui.components.designsystem.theme.CIcons
import org.archivekeep.app.ui.components.feature.InStorageRepositoryDropdownIconLaunched
import org.archivekeep.app.ui.components.feature.StorageDropdownIconLaunched
import org.archivekeep.app.ui.views.storages.StoragesViewState
import org.archivekeep.utils.loading.optional.OptionalLoadable

@Composable
fun StorageEntry(storage: StoragesViewState.Storage) {
    SectionCard {
        SectionCardTitle(
            // TODO
            false,
            storage.displayName,
            icons = {
                StorageDropdownIconLaunched(storage.uri)
            },
        )

        Row(Modifier.sectionCardItem().padding(top = 5.dp, bottom = 8.dp)) {
            ConnectionStatusTag(storage.connectionStatus)
        }

        Spacer(Modifier.height(4.dp))

        SectionCardBottomList(storage.repositoriesInThisStorage) { repository ->
            SectionCardBottomListItem(
                title = repository.displayName,
                statusText = OptionalLoadable.LoadedAvailable(""),
                icon = {
                    Icon(CIcons.Repository, "Repository")
                },
                actions = {
                    InStorageRepositoryDropdownIconLaunched(repository.uri)
                },
            )
        }
    }
}
