package org.archivekeep.files.procedures.sync.discovery

import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.operations.AdditiveReplicationOperation
import org.archivekeep.files.procedures.sync.operations.CopyNewFileOperation
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation
import org.archivekeep.files.repo.Repo

class SyncProcedureDiscovery(
    val relocationSyncMode: RelocationSyncMode,
) {
    suspend fun prepare(
        base: Repo,
        dst: Repo,
    ): DiscoveredSync {
        val comparisonResult = CompareOperation().execute(base, dst)

        return prepareFromComparison(comparisonResult)
    }

    fun prepareFromComparison(comparisonResult: CompareOperation.Result): DiscoveredSync {
        val relocationsSyncStep =
            run {
                if (comparisonResult.hasRelocations) {
                    when (relocationSyncMode) {
                        RelocationSyncMode.Disabled -> DiscoveredRelocationsMoveApplyGroup(emptyList(), comparisonResult.relocations)
                        RelocationSyncMode.AdditiveDuplicating ->
                            DiscoveredAdditiveRelocationsGroup(
                                comparisonResult.relocations.map { AdditiveReplicationOperation(it) },
                            )

                        is RelocationSyncMode.Move -> {
                            fun canBeApplied(relocation: CompareOperation.Result.Relocation): Boolean =
                                if (relocation.isIncreasingDuplicates) {
                                    relocationSyncMode.allowDuplicateIncrease
                                } else if (relocation.isDecreasingDuplicates) {
                                    relocationSyncMode.allowDuplicateReduction
                                } else {
                                    true
                                }

                            DiscoveredRelocationsMoveApplyGroup(
                                toApply =
                                    comparisonResult.relocations.filter { canBeApplied(it) }.map {
                                        RelocationApplyOperation(
                                            it,
                                        )
                                    },
                                toIgnore = comparisonResult.relocations.filter { !canBeApplied(it) },
                            )
                        }
                    }
                } else {
                    null
                }
            }

        val newFilesSyncStep =
            run {
                if (comparisonResult.unmatchedBaseExtras.isNotEmpty()) {
                    DiscoveredNewFilesGroup(
                        comparisonResult.unmatchedBaseExtras.map {
                            CopyNewFileOperation(
                                it,
                            )
                        },
                    )
                } else {
                    null
                }
            }

        val steps =
            listOfNotNull(
                relocationsSyncStep,
                newFilesSyncStep,
            )

        return DiscoveredSync(steps)
    }
}
