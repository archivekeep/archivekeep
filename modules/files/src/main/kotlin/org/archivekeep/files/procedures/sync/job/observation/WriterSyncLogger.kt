package org.archivekeep.files.procedures.sync.job.observation

import java.io.PrintWriter

class WriterSyncLogger(
    val printWriter: PrintWriter,
    val errorPrintWriter: PrintWriter,
) : SyncExecutionObserver {
    override fun onFileStored(filename: String) {
        printWriter.println("file stored: $filename")
        printWriter.flush()
    }

    override fun onFileStoreFailed(
        filename: String,
        cause: Throwable,
    ) {
        errorPrintWriter.println("file store failed: $filename: ${cause.message}")
        errorPrintWriter.flush()
    }

    override fun onFileMoved(
        from: String,
        to: String,
    ) {
        printWriter.println("file moved: $from -> $to")
        printWriter.flush()
    }

    override fun onFileMoveFailed(
        from: String,
        to: String,
        cause: Throwable,
    ) {
        errorPrintWriter.println("file move failed: $from -> $to: ${cause.message}")
        errorPrintWriter.flush()
    }

    override fun onFileDeleted(filename: String) {
        printWriter.println("file deleted: $filename")
        printWriter.flush()
    }

    override fun onFileDeleteFailed(
        filename: String,
        cause: Throwable,
    ) {
        errorPrintWriter.println("file delete failed: $filename: ${cause.message}")
        errorPrintWriter.flush()
    }
}
