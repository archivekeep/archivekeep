package org.archivekeep.files.procedures.sync.log

class NoOpLogger : SyncLogger {
    override fun onFileStored(filename: String) {
    }

    override fun onFileMoved(
        from: String,
        to: String,
    ) {
    }

    override fun onFileDeleted(filename: String) {
    }
}
