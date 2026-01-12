package org.archivekeep.files.procedures.sync.operations

import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.operations.CompareOperation
import org.archivekeep.files.procedures.sync.job.observation.SyncExecutionObserver
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import kotlin.math.min

data class RelocationApplyOperation(
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

        if (relocation.isIncreasingDuplicates) {
            relocation.extraBaseLocations
                .subList(
                    relocation.extraOtherLocations.size,
                    relocation.extraBaseLocations.size,
                ).forEach { extraBaseLocation ->
                    context.runOperation { operationContext ->
                        val success = copyFileAndLog(operationContext, dst, base, extraBaseLocation, logger)

                        if (!success) {
                            result = SyncOperation.ExecutionResult.FAIL
                        }
                    }
                }
        }
        if (relocation.isDecreasingDuplicates) {
            relocation.extraOtherLocations
                .subList(
                    relocation.extraBaseLocations.size,
                    relocation.extraOtherLocations.size,
                ).forEach { extraOtherLocation ->
                    val success = deleteFile(dst, extraOtherLocation, logger)

                    if (!success) {
                        result = SyncOperation.ExecutionResult.FAIL
                    }
                }
        }

        for (i in 0..<min(relocation.extraBaseLocations.size, relocation.extraOtherLocations.size)) {
            val from = relocation.extraOtherLocations[i]
            val to = relocation.extraBaseLocations[i]

            try {
                dst.move(from, to)
                logger.onFileMoved(from, to)
            } catch (e: Throwable) {
                logger.onFileMoveFailed(from, to, e)
                result = SyncOperation.ExecutionResult.FAIL
            }
        }

        return result
    }
}
