package org.archivekeep.files.procedures.sync.job.observation

/**
 * NoOp observer does nothing on events.
 */
class NoOpSyncExecutionObserver : SyncExecutionObserver {
    override fun onFileStored(filename: String) {}

    override fun onFileStoreFailed(
        filename: String,
        cause: Throwable,
    ) {}

    override fun onFileMoved(
        from: String,
        to: String,
    ) {}

    override fun onFileMoveFailed(
        from: String,
        to: String,
        cause: Throwable,
    ) {}

    override fun onFileDeleted(filename: String) {}

    override fun onFileDeleteFailed(
        filename: String,
        cause: Throwable,
    ) {}
}
