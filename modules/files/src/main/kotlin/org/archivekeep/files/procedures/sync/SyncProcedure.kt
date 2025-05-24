package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.progress.CopyOperationProgress
import org.archivekeep.files.procedures.sync.SyncProcedureJobTask.ProgressSummary
import org.archivekeep.files.procedures.sync.operations.AdditiveReplicationOperation
import org.archivekeep.files.procedures.sync.operations.CopyNewFileOperation
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.filesAutoPlural
import org.archivekeep.utils.procedures.operations.OperationContext
import java.time.Duration
import java.time.LocalDateTime
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
                    DiscoveredNewFilesGroup(comparisonResult.unmatchedBaseExtras.map { CopyNewFileOperation(it) })
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

class DiscoveredAdditiveRelocationsGroup(
    steps: List<AdditiveReplicationOperation>,
) : DiscoveredSyncOperationsGroup<AdditiveReplicationOperation>(steps) {
    override fun summaryText(progress: ProgressSummary<AdditiveReplicationOperation>): String =
        "replicated ${progress.completedOperations.size} of ${filesAutoPlural(progress.allOperations)}"
}

class DiscoveredRelocationsMoveApplyGroup(
    toApply: List<RelocationApplyOperation>,
    val toIgnore: List<CompareOperation.Result.Relocation>,
) : DiscoveredSyncOperationsGroup<RelocationApplyOperation>(toApply) {
    override fun summaryText(progress: ProgressSummary<RelocationApplyOperation>): String =
        "relocated ${progress.completedOperations.size} of ${filesAutoPlural(progress.allOperations)}"
}

class DiscoveredNewFilesGroup(
    unmatchedBaseExtras: List<CopyNewFileOperation>,
) : DiscoveredSyncOperationsGroup<CopyNewFileOperation>(unmatchedBaseExtras) {
    override fun summaryText(progress: ProgressSummary<CopyNewFileOperation>): String =
        "copied ${progress.completedOperations.size} of ${filesAutoPlural(progress.allOperations)}"
}

interface SyncLogger {
    fun onFileStored(filename: String)

    fun onFileMoved(
        from: String,
        to: String,
    )

    fun onFileDeleted(filename: String)
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

        val monitor = { progress: Long ->
            progressReport(
                CopyOperationProgress(
                    filename,
                    timeConsumed = Duration.between(timeStarted, LocalDateTime.now()).toKotlinDuration(),
                    copied = progress,
                    total = info.length,
                ),
            )
        }

        progressReport(
            CopyOperationProgress(
                filename,
                timeConsumed = Duration.between(timeStarted, LocalDateTime.now()).toKotlinDuration(),
                copied = 0,
                total = info.length,
            ),
        )

        stream.use {
            dst.save(
                filename,
                info,
                stream,
                monitor = monitor,
            )
        }
    }
}

internal suspend fun copyFileAndLog(
    context: OperationContext,
    dst: Repo,
    base: Repo,
    filename: String,
    logger: SyncLogger,
) {
    copyFile(base, filename, dst, context::progressReport)

    logger.onFileStored(filename)
}

internal suspend fun deleteFile(
    dst: Repo,
    filename: String,
    logger: SyncLogger,
) {
    dst.delete(filename)

    logger.onFileDeleted(filename)
}
