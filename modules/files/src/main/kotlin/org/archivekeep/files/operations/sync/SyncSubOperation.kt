package org.archivekeep.files.operations.sync

import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.files.operations.tasks.InProgressOperationStats
import org.archivekeep.files.repo.Repo

sealed interface SyncSubOperation {
    suspend fun apply(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        inProgressOperationStatsMutableFlow: MutableStateFlow<List<InProgressOperationStats>>,
    )
}
