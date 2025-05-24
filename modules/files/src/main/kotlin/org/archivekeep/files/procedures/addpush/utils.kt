package org.archivekeep.files.procedures.addpush

import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation

fun IndexUpdateProcedure.PreparationResult.Move.toSyncRelocationOperation(): RelocationApplyOperation =
    RelocationApplyOperation(
        CompareOperation.Result.Relocation(
            checksum = this.checksum,
            fileSize = this.fileSize,
            otherFilenames = listOf(from),
            baseFilenames = listOf(to),
        ),
    )
