package org.archivekeep.files.procedures.sync.job.observation

/**
 * NoOp observer does nothing on events.
 */
class NoOpSyncExecutionObserver : SyncExecutionObserver {
    override fun onFileStored(filename: String) {
        // no-op
    }

    override fun onFileStoreFailed(
        filename: String,
        cause: Throwable,
    ) {
        // no-op
    }

    override fun onFileMoved(
        from: String,
        to: String,
    ) {
        // no-op
    }

    override fun onFileMoveFailed(
        from: String,
        to: String,
        cause: Throwable,
    ) {
        // no-op
    }

    override fun onFileDeleted(filename: String) {
        // no-op
    }

    override fun onFileDeleteFailed(
        filename: String,
        cause: Throwable,
    ) {
        // no-op
    }
}
