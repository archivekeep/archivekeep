package org.archivekeep.files.operations.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.sync.AdditiveRelocationsSyncStep.AdditiveReplicationSubOperation
import org.archivekeep.files.operations.sync.NewFilesSyncStep.CopyNewFileSubOperation
import org.archivekeep.files.operations.sync.RelocationsMoveApplySyncStep.RelocationApplySubOperation
import org.archivekeep.files.repo.Repo
import kotlin.math.min

sealed interface RelocationSyncMode {
    data object Disabled : RelocationSyncMode

    data object AdditiveDuplicating : RelocationSyncMode

    data class Move(
        val allowDuplicateIncrease: Boolean,
        val allowDuplicateReduction: Boolean,
    ) : RelocationSyncMode
}

class SyncOperation(
    val relocationSyncMode: RelocationSyncMode,
) {
    suspend fun prepare(
        base: Repo,
        dst: Repo,
    ): PreparedSyncOperation {
        val comparisonResult = CompareOperation().execute(base, dst)

        return prepareFromComparison(comparisonResult)
    }

    fun prepareFromComparison(comparisonResult: CompareOperation.Result): PreparedSyncOperation {
        val relocationsSyncStep =
            run {
                if (comparisonResult.hasRelocations) {
                    when (relocationSyncMode) {
                        RelocationSyncMode.Disabled -> RelocationsMoveApplySyncStep(emptyList(), comparisonResult.relocations)
                        RelocationSyncMode.AdditiveDuplicating ->
                            AdditiveRelocationsSyncStep(
                                comparisonResult.relocations.map { AdditiveReplicationSubOperation(it) },
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

                            RelocationsMoveApplySyncStep(
                                toApply = comparisonResult.relocations.filter { canBeApplied(it) }.map { RelocationApplySubOperation(it) },
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
                    NewFilesSyncStep(comparisonResult.unmatchedBaseExtras.map { CopyNewFileSubOperation(it) })
                } else {
                    null
                }
            }

        val steps =
            listOfNotNull(
                relocationsSyncStep,
                newFilesSyncStep,
            )

        return PreparedSyncOperation(steps)
    }
}

class AdditiveRelocationsSyncStep internal constructor(
    steps: List<AdditiveReplicationSubOperation>,
) : SyncSubOperationGroup<AdditiveReplicationSubOperation>(steps) {
    data class AdditiveReplicationSubOperation(
        val relocation: CompareOperation.Result.Relocation,
    ) : SyncSubOperation {
        override suspend fun apply(
            base: Repo,
            dst: Repo,
            logger: SyncLogger,
        ) {
            relocation.extraBaseLocations.forEach { extraBaseLocation ->
                copyFileAndLog(dst, base, extraBaseLocation, logger)
            }
        }
    }

    override fun constructProgress(
        all: List<AdditiveReplicationSubOperation>,
        completed: List<AdditiveReplicationSubOperation>,
    ): SyncSubOperationGroup.Progress = Progress(all, completed)

    class Progress(
        val allRelocations: List<AdditiveReplicationSubOperation>,
        val completedRelocations: List<AdditiveReplicationSubOperation>,
    ) : SyncSubOperationGroup.Progress {
        override fun summaryText(): String = "replicated ${completedRelocations.size} of ${allRelocations.size}"
    }
}

class RelocationsMoveApplySyncStep internal constructor(
    toApply: List<RelocationApplySubOperation>,
    val toIgnore: List<CompareOperation.Result.Relocation>,
) : SyncSubOperationGroup<RelocationApplySubOperation>(toApply) {
    data class RelocationApplySubOperation(
        val relocation: CompareOperation.Result.Relocation,
    ) : SyncSubOperation {
        override suspend fun apply(
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
                        copyFileAndLog(dst, base, extraBaseLocation, logger)
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

    override fun constructProgress(
        all: List<RelocationApplySubOperation>,
        completed: List<RelocationApplySubOperation>,
    ): SyncSubOperationGroup.Progress = Progress(all, completed)

    class Progress(
        val allRelocations: List<RelocationApplySubOperation>,
        val completedRelocations: List<RelocationApplySubOperation>,
    ) : SyncSubOperationGroup.Progress {
        override fun summaryText(): String = "moved ${completedRelocations.size} of ${allRelocations.size}"
    }
}

class NewFilesSyncStep internal constructor(
    unmatchedBaseExtras: List<CopyNewFileSubOperation>,
) : SyncSubOperationGroup<CopyNewFileSubOperation>(unmatchedBaseExtras) {
    data class CopyNewFileSubOperation(
        val unmatchedBaseExtra: CompareOperation.Result.ExtraGroup,
    ) : SyncSubOperation {
        override suspend fun apply(
            base: Repo,
            dst: Repo,
            logger: SyncLogger,
        ) {
            unmatchedBaseExtra.filenames.forEach { filename ->
                copyFileAndLog(dst, base, filename, logger)
            }
        }
    }

    override fun constructProgress(
        all: List<CopyNewFileSubOperation>,
        completed: List<CopyNewFileSubOperation>,
    ): SyncSubOperationGroup.Progress = Progress(all, completed)

    class Progress(
        val all: List<CopyNewFileSubOperation>,
        val completed: List<CopyNewFileSubOperation>,
    ) : SyncSubOperationGroup.Progress {
        override fun summaryText(): String = "copied ${completed.size} of ${all.size}"
    }
}

interface SyncLogger {
    fun onFileStored(filename: String)

    fun onFileMoved(
        from: String,
        to: String,
    )

    fun onFileDeleted(filename: String)
}

class PreparedSyncOperation internal constructor(
    val steps: List<SyncSubOperationGroup<*>>,
) {
    suspend fun execute(
        base: Repo,
        dst: Repo,
        prompter: suspend (step: SyncSubOperationGroup<*>) -> Boolean,
        logger: SyncLogger,
        progressReport: (progress: List<SyncSubOperationGroup.Progress>) -> Unit = {},
        limitToSubset: Set<SyncSubOperation>? = null,
    ) {
        var finishedStepProgress = listOf<SyncSubOperationGroup.Progress>()

        steps.forEach { step ->
            val confirmed = prompter(step)

            if (!confirmed) {
                throw RuntimeException("abandoned")
            }

            val resultProgress =
                step.execute(base, dst, logger, progressReport = {
                    progressReport(finishedStepProgress + listOf(it))
                }, limitToSubset)

            finishedStepProgress = finishedStepProgress + listOf(resultProgress)
        }

        progressReport(finishedStepProgress)
    }

    fun isNoOp(): Boolean = steps.isEmpty() || steps.all { it.isNoOp() }
}

suspend fun copyFile(
    base: Repo,
    filename: String,
    dst: Repo,
) {
    withContext(Dispatchers.IO) {
        val (info, stream) = base.open(filename)

        stream.use {
            dst.save(filename, info, stream)
        }
    }
}

private suspend fun copyFileAndLog(
    dst: Repo,
    base: Repo,
    filename: String,
    logger: SyncLogger,
) {
    copyFile(base, filename, dst)

    logger.onFileStored(filename)
}

private suspend fun deleteFile(
    dst: Repo,
    filename: String,
    logger: SyncLogger,
) {
    dst.delete(filename)

    logger.onFileDeleted(filename)
}
