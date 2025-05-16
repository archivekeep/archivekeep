package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.progress.CopyOperationProgress
import org.archivekeep.files.procedures.sync.AdditiveRelocationsSyncStep.AdditiveReplicationOperation
import org.archivekeep.files.procedures.sync.NewFilesSyncStep.CopyNewFileOperation
import org.archivekeep.files.procedures.sync.RelocationsMoveApplySyncStep.RelocationApplyOperation
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.filesAutoPlural
import org.archivekeep.utils.procedures.OperationContext
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.min
import kotlin.time.toKotlinDuration

sealed interface RelocationSyncMode {
    data object Disabled : RelocationSyncMode

    data object AdditiveDuplicating : RelocationSyncMode

    data class Move(
        val allowDuplicateIncrease: Boolean,
        val allowDuplicateReduction: Boolean,
    ) : RelocationSyncMode
}

class SyncProcedure(
    val relocationSyncMode: RelocationSyncMode,
) {
    suspend fun prepare(
        base: Repo,
        dst: Repo,
    ): PreparedSyncProcedure {
        val comparisonResult = CompareOperation().execute(base, dst)

        return prepareFromComparison(comparisonResult)
    }

    fun prepareFromComparison(comparisonResult: CompareOperation.Result): PreparedSyncProcedure {
        val relocationsSyncStep =
            run {
                if (comparisonResult.hasRelocations) {
                    when (relocationSyncMode) {
                        RelocationSyncMode.Disabled -> RelocationsMoveApplySyncStep(emptyList(), comparisonResult.relocations)
                        RelocationSyncMode.AdditiveDuplicating ->
                            AdditiveRelocationsSyncStep(
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

                            RelocationsMoveApplySyncStep(
                                toApply = comparisonResult.relocations.filter { canBeApplied(it) }.map { RelocationApplyOperation(it) },
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
                    NewFilesSyncStep(comparisonResult.unmatchedBaseExtras.map { CopyNewFileOperation(it) })
                } else {
                    null
                }
            }

        val steps =
            listOfNotNull(
                relocationsSyncStep,
                newFilesSyncStep,
            )

        return PreparedSyncProcedure(steps)
    }
}

class AdditiveRelocationsSyncStep internal constructor(
    steps: List<AdditiveReplicationOperation>,
) : SyncOperationGroup<AdditiveReplicationOperation>(steps) {
    data class AdditiveReplicationOperation(
        val relocation: CompareOperation.Result.Relocation,
    ) : SyncOperation {
        override suspend fun apply(
            context: ProcedureExecutionContext,
            base: Repo,
            dst: Repo,
            logger: SyncLogger,
        ) {
            relocation.extraBaseLocations.forEach { extraBaseLocation ->
                context.runOperation { context ->
                    copyFileAndLog(context, dst, base, extraBaseLocation, logger)
                }
            }
        }
    }

    override fun constructProgress(
        all: List<AdditiveReplicationOperation>,
        completed: List<AdditiveReplicationOperation>,
    ): SyncOperationGroup.Progress = Progress(all, completed)

    class Progress(
        val allRelocations: List<AdditiveReplicationOperation>,
        val completedRelocations: List<AdditiveReplicationOperation>,
    ) : SyncOperationGroup.Progress {
        override fun summaryText(): String = "replicated ${completedRelocations.size} of ${allRelocations.size}"

        override fun progress(): Float = completedRelocations.size / allRelocations.size.toFloat()
    }
}

class RelocationsMoveApplySyncStep internal constructor(
    toApply: List<RelocationApplyOperation>,
    val toIgnore: List<CompareOperation.Result.Relocation>,
) : SyncOperationGroup<RelocationApplyOperation>(toApply) {
    data class RelocationApplyOperation(
        val relocation: CompareOperation.Result.Relocation,
    ) : SyncOperation {
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

    override fun constructProgress(
        all: List<RelocationApplyOperation>,
        completed: List<RelocationApplyOperation>,
    ): SyncOperationGroup.Progress = Progress(all, completed)

    class Progress(
        val allRelocations: List<RelocationApplyOperation>,
        val completedRelocations: List<RelocationApplyOperation>,
    ) : SyncOperationGroup.Progress {
        override fun summaryText(): String = "moved ${completedRelocations.size} of ${allRelocations.size}"

        override fun progress(): Float = completedRelocations.size / allRelocations.size.toFloat()
    }
}

class NewFilesSyncStep internal constructor(
    unmatchedBaseExtras: List<CopyNewFileOperation>,
) : SyncOperationGroup<CopyNewFileOperation>(unmatchedBaseExtras) {
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
    }

    override fun constructProgress(
        all: List<CopyNewFileOperation>,
        completed: List<CopyNewFileOperation>,
    ): SyncOperationGroup.Progress = Progress(all, completed)

    class Progress(
        val all: List<CopyNewFileOperation>,
        val completed: List<CopyNewFileOperation>,
    ) : SyncOperationGroup.Progress {
        override fun summaryText(): String = "copied ${completed.size} of ${all.size} ${filesAutoPlural(all)}"

        override fun progress(): Float = completed.size / all.size.toFloat()
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

class PreparedSyncProcedure internal constructor(
    val steps: List<SyncOperationGroup<*>>,
) {
    fun createJob(
        base: Repo,
        dst: Repo,
        prompter: suspend (step: SyncOperationGroup<*>) -> Boolean,
        logger: SyncLogger,
        limitToSubset: Set<SyncOperation>? = null,
    ) = SyncProcedureJob(
        steps,
        base,
        dst,
        prompter,
        logger,
        limitToSubset,
    )

    fun isNoOp(): Boolean = steps.isEmpty() || steps.all { it.isNoOp() }
}

suspend fun copyFile(
    base: Repo,
    filename: String,
    dst: Repo,
    progressReport: (progress: CopyOperationProgress) -> Unit,
) {
    withContext(Dispatchers.IO) {
        val timeStarted = LocalDateTime.now()

        val (info, stream) = base.open(filename)

        stream.use {
            dst.save(
                filename,
                info,
                stream,
                monitor = {
                    progressReport(
                        CopyOperationProgress(
                            filename,
                            timeConsumed = Duration.between(timeStarted, LocalDateTime.now()).toKotlinDuration(),
                            copied = it,
                            total = info.length,
                        ),
                    )
                },
            )
        }
    }
}

private suspend fun copyFileAndLog(
    context: OperationContext,
    dst: Repo,
    base: Repo,
    filename: String,
    logger: SyncLogger,
) {
    copyFile(base, filename, dst, context::progressReport)

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
