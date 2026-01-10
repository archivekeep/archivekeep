package org.archivekeep.files.procedures.sync.operations

import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.job.observation.SyncExecutionObserver
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.ProcedureExecutionContext

data class CopyNewFileOperation(
    val unmatchedBaseExtra: CompareOperation.Result.ExtraGroup,
) : SyncOperation {
    override suspend fun apply(
        context: ProcedureExecutionContext,
        base: Repo,
        dst: Repo,
        logger: SyncExecutionObserver,
    ): SyncOperation.ExecutionResult {
        var result = SyncOperation.ExecutionResult.SUCCESS

        unmatchedBaseExtra.filenames.forEach { filename ->
            context.runOperation { operationContext ->
                val success = copyFileAndLog(operationContext, dst, base, filename, logger)

                if (!success) {
                    result = SyncOperation.ExecutionResult.FAIL
                }
            }
        }

        return result
    }

    override val bytesToCopy: Long? = unmatchedBaseExtra.fileSize?.times(unmatchedBaseExtra.filenames.size)
}
