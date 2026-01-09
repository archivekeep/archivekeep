package org.archivekeep.files.procedures.sync.operations

import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.log.SyncLogger
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.ProcedureExecutionContext

data class CopyNewFileOperation(
    val unmatchedBaseExtra: CompareOperation.Result.ExtraGroup,
) : SyncOperation {
    override suspend fun apply(
        context: ProcedureExecutionContext,
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
    ) {
        unmatchedBaseExtra.filenames.forEach { filename ->
            context.runOperation { operationContext ->
                copyFileAndLog(operationContext, dst, base, filename, logger)
            }
        }
    }

    override val bytesToCopy: Long? = unmatchedBaseExtra.fileSize?.times(unmatchedBaseExtra.filenames.size)
}
