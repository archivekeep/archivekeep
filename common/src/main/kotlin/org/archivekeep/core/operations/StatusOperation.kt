package org.archivekeep.core.operations

import org.archivekeep.core.repo.LocalRepo
import org.archivekeep.core.repo.Repo
import kotlin.io.path.invariantSeparatorsPathString

class NotLocalRepoException : RuntimeException("not local repo")

class StatusOperation(
    val subsetGlobs: List<String>,
) {
    suspend fun execute(repo: Repo): Result {
        val localRepo = repo as? LocalRepo ?: throw NotLocalRepoException()

        val matchedFiles = localRepo.findAllFiles(subsetGlobs)

        class FileStatus(
            val filename: String,
            val indexed: Boolean,
        )

        val matchedFilesStatus =
            matchedFiles.map {
                val invariantFilename = it.invariantSeparatorsPathString

                FileStatus(
                    filename = invariantFilename,
                    indexed = localRepo.contains(invariantFilename),
                )
            }

        val newFiles = matchedFilesStatus.filter { !it.indexed }.map { it.filename }
        val storedFiles = matchedFilesStatus.filter { it.indexed }.map { it.filename }

        return Result(
            newFiles = newFiles,
            storedFiles = storedFiles,
        )
    }

    class Result(
        val newFiles: List<String>,
        val storedFiles: List<String>,
    ) {
        val hasChanges: Boolean
            get() = newFiles.isNotEmpty()
    }
}
