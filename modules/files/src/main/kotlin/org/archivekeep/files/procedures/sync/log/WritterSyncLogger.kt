package org.archivekeep.files.procedures.sync.log

import java.io.PrintWriter

class WritterSyncLogger(
    val printWriter: PrintWriter,
) : SyncLogger {
    override fun onFileStored(filename: String) {
        printWriter.println("file stored: $filename")
        printWriter.flush()
    }

    override fun onFileMoved(
        from: String,
        to: String,
    ) {
        printWriter.println("file moved: $from -> $to")
        printWriter.flush()
    }

    override fun onFileDeleted(filename: String) {
        printWriter.println("file deleted: $filename")
        printWriter.flush()
    }
}
