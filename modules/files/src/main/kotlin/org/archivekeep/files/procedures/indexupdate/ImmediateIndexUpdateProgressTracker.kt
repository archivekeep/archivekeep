package org.archivekeep.files.procedures.indexupdate

interface ImmediateIndexUpdateProgressTracker {
    fun onAddCompleted(newFile: String)

    fun onMoveIgnored(
        newFile: String,
        oldLocations: List<String>,
    )

    fun onDuplicationIgnored(
        newFile: String,
        existingFiles: List<String>,
    )
}
