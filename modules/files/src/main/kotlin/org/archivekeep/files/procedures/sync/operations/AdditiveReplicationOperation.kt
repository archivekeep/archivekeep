package org.archivekeep.files.procedures.sync.operations

import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.log.SyncLogger
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.ProcedureExecutionContext

data class AdditiveReplicationOperation(
    val relocation: CompareOperation.Result.Relocation,
) : SyncOperation {
    override val bytesToCopy: Long? = relocation.fileSize?.times(relocation.extraBaseLocations.size)

    override suspend fun apply(
        context: ProcedureExecutionContext,
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
    ) {
        relocation.extraBaseLocations.forEach { extraBaseLocation ->
            context.runOperation { context ->
                copyFileAndLog(context, dst, base, extraBaseLocation, logger)
            }
        }
    }
}
