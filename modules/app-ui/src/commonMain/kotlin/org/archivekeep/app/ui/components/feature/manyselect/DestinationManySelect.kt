package org.archivekeep.app.ui.components.feature.manyselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.DestinationManySelect(
    allFiles: List<StorageRepository>,
    selectedFilenames: MutableState<Set<RepositoryURI>>,
) {
    val launchers = LocalArchiveOperationLaunchers.current

    val repositoryManySelect =
        rememberManySelect(allFiles, selectedFilenames, keyMapper = { it.uri })

    LabelText("Repositories to push to:")

    Spacer(modifier = Modifier.height(6.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repositoryManySelect.allItems.forEach { repository ->
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                CheckboxWithText(
                    repositoryManySelect.selectedItems.contains(repository.uri),
                    text = repository.storage.displayName,
                    onValueChange = {
                        repositoryManySelect.onItemChange(repository.uri, it)
                    },
                    extraItems = {
                        if (repository.repositoryState.connectionState.isLocked) {
                            OutlinedButton(onClick = { launchers.unlockRepository(repository.uri, null) }) {
                                Text("Unlock")
                            }
                        }
                    },
                )
            }
        }
    }
}
