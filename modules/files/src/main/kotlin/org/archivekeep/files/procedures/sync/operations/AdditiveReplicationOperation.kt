package org.archivekeep.files.procedures.sync.operations

import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.job.observation.SyncExecutionObserver
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
        logger: SyncExecutionObserver,
    ): SyncOperation.ExecutionResult {
        var result = SyncOperation.ExecutionResult.SUCCESS

        relocation.extraBaseLocations.forEach { extraBaseLocation ->
            context.runOperation { context ->
                val success = copyFileAndLog(context, dst, base, extraBaseLocation, logger)

                if (!success) {
                    result = SyncOperation.ExecutionResult.FAIL
                }
            }
        }

        return result
    }
}
