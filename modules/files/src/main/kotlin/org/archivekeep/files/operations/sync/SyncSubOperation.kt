package org.archivekeep.files.operations.sync

import org.archivekeep.files.repo.Repo

sealed interface SyncSubOperation {
    suspend fun apply(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
    )
}
