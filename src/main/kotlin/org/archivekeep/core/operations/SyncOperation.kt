package org.archivekeep.core.operations

import org.archivekeep.core.repo.Repo
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
    val relocationSyncMode: RelocationSyncMode
) {
    fun prepare(
        base: Repo,
        dst: Repo,
    ): PreparedSyncOperation {
        val comparisonResult = CompareOperation().execute(base, dst)

        return prepareFromComparison(comparisonResult)
    }

    fun prepareFromComparison(comparisonResult: CompareOperation.Result): PreparedSyncOperation {
        val relocationsSyncStep = run {
            if (comparisonResult.hasRelocations) {
                when (relocationSyncMode) {
                    RelocationSyncMode.Disabled -> throw RelocationsPresentButDisabledException
                    RelocationSyncMode.AdditiveDuplicating -> AdditiveRelocationsSyncStep(comparisonResult.relocations)
                    is RelocationSyncMode.Move -> {
                        if (comparisonResult.relocations.any { it.isIncreasingDuplicates } && !relocationSyncMode.allowDuplicateIncrease) {
                            throw DuplicationIncreasePresentButDisabledException
                        }

                        if (comparisonResult.relocations.any { it.isDecreasingDuplicates } && !relocationSyncMode.allowDuplicateReduction) {
                            throw RuntimeException("duplicate reduction is not allowed")
                        }

                        RelocationsMoveApplySyncStep(comparisonResult.relocations)
                    }
                }
            } else null
        }

        val newFilesSyncStep = run {
            if (comparisonResult.unmatchedBaseExtras.isNotEmpty()) {
                NewFilesSyncStep(comparisonResult.unmatchedBaseExtras)
            } else null
        }

        val steps = listOfNotNull(
            relocationsSyncStep, newFilesSyncStep
        )

        return PreparedSyncOperation(
            steps
        )
    }
}

sealed interface SyncPlanStep {
    fun execute(
        base: Repo,
        dst: Repo,
        logger: SyncLogger,
    )
}

class AdditiveRelocationsSyncStep internal constructor(
    val relocations: List<CompareOperation.Result.Relocation>
) : SyncPlanStep {
    override fun execute(base: Repo, dst: Repo, logger: SyncLogger) {
        relocations.forEach { relocation ->
            relocation.extraBaseLocations.forEach { extraBaseLocation ->
                copyFile(dst, base, extraBaseLocation, logger)
            }
        }
    }

}

class RelocationsMoveApplySyncStep internal constructor(
    val relocations: List<CompareOperation.Result.Relocation>
) : SyncPlanStep {
    override fun execute(base: Repo, dst: Repo, logger: SyncLogger) {
        relocations.forEach { relocation ->
            if (relocation.isIncreasingDuplicates) {
                relocation.extraBaseLocations.subList(
                    relocation.extraOtherLocations.size, relocation.extraBaseLocations.size
                ).forEach { extraBaseLocation ->
                    copyFile(dst, base, extraBaseLocation, logger)
                }
            }
            if (relocation.isDecreasingDuplicates) {
                relocation.extraOtherLocations.subList(
                    relocation.extraBaseLocations.size, relocation.extraOtherLocations.size
                ).forEach { extraOtherLocation ->
                    deleteFile(dst, extraOtherLocation)
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
}

class NewFilesSyncStep internal constructor(
    val unmatchedBaseExtras: List<CompareOperation.Result.ExtraGroup>
) : SyncPlanStep {
    override fun execute(base: Repo, dst: Repo, logger: SyncLogger) {
        unmatchedBaseExtras.forEach { unmatchedBaseExtra ->
            unmatchedBaseExtra.filenames.forEach { filename ->
                copyFile(dst, base, filename, logger)
            }
        }
    }
}

interface SyncLogger {
    fun onFileStored(filename: String)

    fun onFileMoved(from: String, to: String)
}

class PreparedSyncOperation internal constructor(
    val steps: List<SyncPlanStep>
) {
    fun execute(
        base: Repo,
        dst: Repo,
        prompter: (step: SyncPlanStep) -> Boolean,
        logger: SyncLogger
    ) {
        steps.forEach { step ->
            val confirmed = prompter(step)

            if (!confirmed) {
                throw RuntimeException("abandoned")
            }

            step.execute(base, dst, logger)
        }
    }
}


private fun copyFile(dst: Repo, base: Repo, filename: String, logger: SyncLogger) {
    val (info, stream) = base.open(filename)

    stream.use {
        dst.save(filename, info, stream)
    }

    logger.onFileStored(filename)
}

private fun deleteFile(dst: Repo, extraOtherLocation: String) {
    TODO("Not yet implemented")
}

object RelocationsPresentButDisabledException: RuntimeException("relocations disabled but present")

object DuplicationIncreasePresentButDisabledException: RuntimeException("duplicate increase is not allowed")
