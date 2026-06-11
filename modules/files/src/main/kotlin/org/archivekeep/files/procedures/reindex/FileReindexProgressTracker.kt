package org.archivekeep.files.procedures.reindex

interface FileReindexProgressTracker {
    fun onFileReindexed(reindexedFile: String)
}
