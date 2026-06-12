package org.archivekeep.files.procedures.deletedcleanup

interface DeletedFilesCleanupProgressTracker {
    fun onFileRemoved(removedFile: String)
}
