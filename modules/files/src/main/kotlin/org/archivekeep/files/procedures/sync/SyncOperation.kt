package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.files.procedures.progress.OperationProgress
import org.archivekeep.files.repo.Repo

sealed interface SyncOperation {
    suspend fun apply(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        operationProgressMutableFlow: MutableStateFlow<List<OperationProgress>>,
    )
}
