package org.archivekeep.files.procedures.sync.operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archivekeep.files.procedures.progress.CopyOperationProgress
import org.archivekeep.files.procedures.sync.log.SyncLogger
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.operations.OperationContext
import java.time.Duration
import java.time.LocalDateTime
import kotlin.time.toKotlinDuration

suspend fun copyFile(
    base: Repo,
    filename: String,
    dst: Repo,
    progressReport: (progress: CopyOperationProgress) -> Unit,
) {
    withContext(Dispatchers.IO) {
        val timeStarted = LocalDateTime.now()

        base.open(filename) { info, stream ->

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
