package org.archivekeep.app.ui.views.home.model

import org.archivekeep.app.core.domain.repositories.RepositoryConnectionState
import org.archivekeep.app.ui.utils.combineTexts
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.isLoading
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.text.filesAutoPlural

data class HomeLocalArchiveSecondaryRepositoryUiState(
    val repo: HomeLocalArchiveSecondaryRepositoryModel,
    val localRepoStatus: OptionalLoadable<StatusOperation.Result.Summary>,
    val connectionStatus: RepositoryConnectionState,
    val syncRunning: Boolean,
    val canPushLoadable: OptionalLoadable<Boolean>,
    val canPull: Boolean,
    val syncTexts: OptionalLoadable<List<String>>,
) {
    val needsUnlock = connectionStatus is RepositoryConnectionState.ConnectedLocked

    val addTexts =
        localRepoStatus.mapLoadedData {
            if (it.totalNewFiles > 0) {
                listOf("Uncommitted ${filesAutoPlural(it.totalNewFiles)}")
            } else {
                emptyList()
            }
        }

    val texts: OptionalLoadable<String> =
        combineTexts(
            OptionalLoadable.LoadedAvailable(
                if (!connectionStatus.isConnected) listOf("Disconnected") else emptyList(),
            ),
            addTexts,
            syncTexts,
        ).mapLoadedData { it.joinToString(", ") }

    val isLoading = syncTexts.isLoading || syncRunning || localRepoStatus.isLoading

    val canAdd = localRepoStatus.mapIfLoadedOrNull { it.totalNewFiles > 0 } ?: false
    val canReindex = localRepoStatus.mapIfLoadedOrNull { it.totalModifiedIndexedFiles > 0 } ?: false
    val canCleanupDeletedFiles = localRepoStatus.mapIfLoadedOrNull { it.totalMissingFiles > 0 } ?: false
    val canPush = canPushLoadable.mapIfLoadedOrNull { it } ?: false
}
