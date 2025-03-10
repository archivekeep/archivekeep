package org.archivekeep.files.operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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
                        RelocationSyncMode.AdditiveDuplicating -> AdditiveRelocationsSyncStep(comparisonResult.relocations)

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
                                toApply = comparisonResult.relocations.filter { canBeApplied(it) },
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
                    NewFilesSyncStep(comparisonResult.unmatchedBaseExtras)
                } else {
                    null
                }
            }

        val steps =
            listOfNotNull(
                relocationsSyncStep,
                newFilesSyncStep,
            )

        return PreparedSyncOperation(
            steps,
        )
    }
}

sealed interface SyncPlanStep {
    suspend fun execute(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        progressReport: (progress: Progress) -> Unit,
    ): SyncPlanStep.Progress

    fun isNoOp(): Boolean

    sealed interface Progress {
        // TODO: localization
        fun summaryText(): String
    }
}

class AdditiveRelocationsSyncStep internal constructor(
    val relocations: List<CompareOperation.Result.Relocation>,
) : SyncPlanStep {
    override suspend fun execute(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        progressReport: (progress: SyncPlanStep.Progress) -> Unit,
    ): SyncPlanStep.Progress {
        var completedRelocations = emptyList<CompareOperation.Result.Relocation>()

        relocations.forEach { relocation ->
            relocation.extraBaseLocations.forEach { extraBaseLocation ->
                copyFileAndLog(dst, base, extraBaseLocation, logger)
            }

            completedRelocations = completedRelocations + listOf(relocation)
            progressReport(
                Progress(relocations, completedRelocations),
            )

            yield()
        }

        return Progress(relocations, completedRelocations)
    }

    override fun isNoOp() = relocations.isEmpty()

    class Progress(
        val allRelocations: List<CompareOperation.Result.Relocation>,
        val completedRelocations: List<CompareOperation.Result.Relocation>,
    ) : SyncPlanStep.Progress {
        override fun summaryText(): String = "replicated ${completedRelocations.size} of ${allRelocations.size}"
    }
}

class RelocationsMoveApplySyncStep internal constructor(
    val toApply: List<CompareOperation.Result.Relocation>,
    val toIgnore: List<CompareOperation.Result.Relocation>,
) : SyncPlanStep {
    override suspend fun execute(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        progressReport: (progress: SyncPlanStep.Progress) -> Unit,
    ): SyncPlanStep.Progress {
        var completedRelocations = emptyList<CompareOperation.Result.Relocation>()

        toApply.forEach { relocation ->
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

            completedRelocations = completedRelocations + listOf(relocation)
            progressReport(
                Progress(toApply, completedRelocations),
            )
        }

        return Progress(toApply, completedRelocations)
    }

    override fun isNoOp() = toApply.isEmpty()

    class Progress(
        val allRelocations: List<CompareOperation.Result.Relocation>,
        val completedRelocations: List<CompareOperation.Result.Relocation>,
    ) : SyncPlanStep.Progress {
        override fun summaryText(): String = "moved ${completedRelocations.size} of ${allRelocations.size}"
    }
}

class NewFilesSyncStep internal constructor(
    val unmatchedBaseExtras: List<CompareOperation.Result.ExtraGroup>,
) : SyncPlanStep {
    override suspend fun execute(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
        progressReport: (progress: SyncPlanStep.Progress) -> Unit,
    ): SyncPlanStep.Progress {
        var completed = emptyList<CompareOperation.Result.ExtraGroup>()

        unmatchedBaseExtras.forEach { unmatchedBaseExtra ->
            unmatchedBaseExtra.filenames.forEach { filename ->
                copyFileAndLog(dst, base, filename, logger)
            }

            completed = completed + listOf(unmatchedBaseExtra)
            progressReport(
                Progress(unmatchedBaseExtras, completed),
            )

            yield()
        }

        return Progress(unmatchedBaseExtras, completed)
    }

    override fun isNoOp() = unmatchedBaseExtras.isEmpty()

    class Progress(
        val all: List<CompareOperation.Result.ExtraGroup>,
        val completed: List<CompareOperation.Result.ExtraGroup>,
    ) : SyncPlanStep.Progress {
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
    val steps: List<SyncPlanStep>,
) {
    suspend fun execute(
        base: Repo,
        dst: Repo,
        prompter: suspend (step: SyncPlanStep) -> Boolean,
        logger: SyncLogger,
        progressReport: (progress: List<SyncPlanStep.Progress>) -> Unit = {},
    ) {
        var finishedStepProgress = listOf<SyncPlanStep.Progress>()

        steps.forEach { step ->
            val confirmed = prompter(step)

            if (!confirmed) {
                throw RuntimeException("abandoned")
            }

            val resultProgress =
                step.execute(base, dst, logger, progressReport = {
                    progressReport(finishedStepProgress + listOf(it))
                })

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
