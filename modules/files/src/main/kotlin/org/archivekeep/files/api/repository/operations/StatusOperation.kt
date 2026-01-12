package org.archivekeep.files.api.repository.operations

import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.Repo
import kotlin.io.path.invariantSeparatorsPathString

class NotLocalRepoException : RuntimeException("not local repo")

class StatusOperation(
    val subsetGlobs: List<String>,
) {
    suspend fun execute(repo: Repo): Result {
        val localRepo = repo as? LocalRepo ?: throw NotLocalRepoException()

        val matchedFiles = localRepo.findAllFiles(subsetGlobs)
        val storedFilesInRepo = localRepo.indexedFilenames()

        class FileStatus(
            val filename: String,
            val indexed: Boolean,
        )

        val matchedFilesStatus =
            matchedFiles.map {
                val invariantFilename = it.invariantSeparatorsPathString

                FileStatus(
                    filename = invariantFilename,
                    indexed = storedFilesInRepo.contains(invariantFilename),
                )
            }

        val newFiles = matchedFilesStatus.filter { !it.indexed }.map { it.filename }
        val indexedFiles = matchedFilesStatus.filter { it.indexed }.map { it.filename }

        return Result(
            newFiles = newFiles,
            indexedFiles = indexedFiles,
        )
    }

    data class Result(
        val newFiles: List<String>,
        val indexedFiles: List<String>,
    ) {
        val hasChanges: Boolean
            get() = newFiles.isNotEmpty()

        val summary = Summary(newFiles.size)

        data class Summary(
            val totalNewFiles: Int,
        )
    }
}
