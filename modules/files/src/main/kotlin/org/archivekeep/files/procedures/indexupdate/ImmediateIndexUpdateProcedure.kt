package org.archivekeep.files.procedures.indexupdate

import org.archivekeep.files.api.exceptions.InvalidFilename
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.Repo
import java.io.PrintWriter

class ImmediateIndexUpdateProcedure {
    suspend fun execute(
        repo: Repo,
        filesToProcess: Set<String>,
        out: PrintWriter? = null,
        vararg progressTrackers: ImmediateIndexUpdateProgressTracker,
    ) {
        val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

        val performanceTracker = RepoPerformanceTracker.DefaultImpl()

        try {
            // TODO: this can actually lead to bad reporting, because it can be lower than original filesToProcess
            val unindexedFilesMatchingPattern =
                filesToProcess.filter {
                    !localRepo.contains(it)
                }

            val filesWithWrongFilenames =
                unindexedFilesMatchingPattern
                    .mapNotNull {
                        it.let { pathString ->
                            illegalCharacters
                                .firstOrNull { illegalCharacter -> pathString.contains(illegalCharacter) }
                                ?.let { illegalCharacter ->
                                    pathString to InvalidFilename(pathString, "$pathString contains illegal character $illegalCharacter")
                                }
                        }
                    }.toMap<String, Any>()

            // TODO: report error

            val filesToActuallyProcess = (unindexedFilesMatchingPattern.toSet() - filesWithWrongFilenames.keys).sorted()

            filesToActuallyProcess.forEach { fileToProcess ->
                val preparedAdd = localRepo.prepareAdd(fileToProcess, performanceTracker)

                val existingFiles = localRepo.getFilesBySha256(preparedAdd.checksumSha256)
                val missingFiles = existingFiles.filter { localRepo.verifyFileExists(it.path) }

                when {
                    missingFiles.isNotEmpty() -> {
                        progressTrackers.forEach { t -> t.onMoveIgnored(fileToProcess, missingFiles.map { it.path }) }
                    }
                    existingFiles.isNotEmpty() -> {
                        progressTrackers.forEach { t -> t.onDuplicationIgnored(fileToProcess, existingFiles.map { it.path }) }
                    }
                    else -> {
                        preparedAdd.add(reindex = false)
                        progressTrackers.forEach { t -> t.onAddCompleted(fileToProcess) }
                    }
                }

                preparedAdd.add(reindex = true)
            }
        } finally {
            performanceTracker.dump(out)
        }
    }
}
