package org.archivekeep.files.procedures.sync

import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.ProcedureExecutionContext

sealed interface SyncOperation {
    suspend fun apply(
        context: ProcedureExecutionContext,
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
    )
}
