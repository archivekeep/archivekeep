package org.archivekeep.files.procedures.sync.operations

import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.SyncLogger
import org.archivekeep.files.procedures.sync.copyFileAndLog
import org.archivekeep.files.procedures.sync.deleteFile
import org.archivekeep.files.repo.Repo
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
        logger: SyncLogger,
    ) {
        if (relocation.isIncreasingDuplicates) {
            relocation.extraBaseLocations
                .subList(
                    relocation.extraOtherLocations.size,
                    relocation.extraBaseLocations.size,
                ).forEach { extraBaseLocation ->
                    context.runOperation { operationContext ->
                        copyFileAndLog(operationContext, dst, base, extraBaseLocation, logger)
                    }
                }
        }
        if (relocation.isDecreasingDuplicates) {
            relocation.extraOtherLocations
                .subList(
                    relocation.extraBaseLocations.size,
                    relocation.extraOtherLocations.size,
                ).forEach { extraOtherLocation ->
                    deleteFile(dst, extraOtherLocation, logger)
                }
        }

        for (i in 0..<min(relocation.extraBaseLocations.size, relocation.extraOtherLocations.size)) {
            val from = relocation.extraOtherLocations[i]
            val to = relocation.extraBaseLocations[i]

            dst.move(from, to)
            logger.onFileMoved(from, to)
        }
    }
}
