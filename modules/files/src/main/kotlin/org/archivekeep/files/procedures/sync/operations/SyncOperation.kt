package org.archivekeep.files.procedures.sync.operations

import org.archivekeep.files.procedures.sync.SyncLogger
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.ProcedureExecutionContext

sealed interface SyncOperation {
    val bytesToCopy: Long?

    suspend fun apply(
        context: ProcedureExecutionContext,
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
    )
}
