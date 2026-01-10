package org.archivekeep.files.procedures.sync.job.observation

interface SyncExecutionObserver {
    fun onFileStored(filename: String)

    fun onFileStoreFailed(
        filename: String,
        cause: Throwable,
    )

    fun onFileMoved(
        from: String,
        to: String,
    )

    fun onFileMoveFailed(
        from: String,
        to: String,
        cause: Throwable,
    )

    fun onFileDeleted(filename: String)

    fun onFileDeleteFailed(
        filename: String,
        cause: Throwable,
    )
}
