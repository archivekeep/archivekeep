package org.archivekeep.core.operations

import org.archivekeep.core.repo.LocalRepo
import org.archivekeep.core.repo.Repo
import kotlin.io.path.invariantSeparatorsPathString

val illegalCharacters =
    listOf(
        ":",
        "?",
        "<",
        ">",
        "*",
        "|",
    )

class AddOperation(
    val subsetGlobs: List<String>,
    val disableFilenameCheck: Boolean,
    val disableMovesCheck: Boolean,
) {
    fun prepare(repo: Repo): PreparationResult {
        val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

        val matchedFiles = localRepo.findAllFiles(subsetGlobs)

        val unindexedFilesMatchingPattern =
            matchedFiles.filter {
                !localRepo.contains(it.invariantSeparatorsPathString)
            }

        if (!disableFilenameCheck) {
            unindexedFilesMatchingPattern.forEach {
                it.invariantSeparatorsPathString.let { pathString ->
                    illegalCharacters
                        .firstOrNull { illegalCharacter -> pathString.contains(illegalCharacter) }
                        ?.let { illegalCharacter ->
                            throw RuntimeException("$pathString contains illegal character $illegalCharacter")
                        }
                }
            }
        }

        if (!disableMovesCheck) {
            val storedFiles = localRepo.storedFiles().sorted()

            val missingIndexedFilesByChecksum =
                storedFiles
                    .filter { !localRepo.verifyFileExists(it) }
                    .associateBy {
                        localRepo.fileChecksum(it)
                    }

            val remainingMissingIndexedFilesByChecksum = missingIndexedFilesByChecksum.toMutableMap()
            val moves = mutableListOf<PreparationResult.Move>()
            val unmatchedNewFiles = mutableListOf<String>()

            unindexedFilesMatchingPattern.forEach { newUnindexedFile ->
                val checksum = localRepo.computeFileChecksum(newUnindexedFile)

                val missingFilePath = remainingMissingIndexedFilesByChecksum[checksum]

                if (missingFilePath != null) {
                    moves.add(
                        PreparationResult.Move(
                            from = missingFilePath,
                            to = newUnindexedFile.invariantSeparatorsPathString,
                        ),
                    )
                    remainingMissingIndexedFilesByChecksum.remove(checksum)
                } else {
                    unmatchedNewFiles.add(newUnindexedFile.invariantSeparatorsPathString)
                }
            }

            return PreparationResult(
                newFiles = unmatchedNewFiles.sorted(),
                moves = moves,
                missingFiles = remainingMissingIndexedFilesByChecksum.values.toList().sorted(),
            )
        } else {
            return PreparationResult(
                unindexedFilesMatchingPattern.map { it.invariantSeparatorsPathString }.sorted(),
                emptyList(),
                emptyList(),
            )
        }
    }

    data class PreparationResult(
        val newFiles: List<String>,
        val moves: List<Move>,
        val missingFiles: List<String>,
    ) {
        data class Move(
            val from: String,
            val to: String,
        )

        fun executeMoves(
            repo: Repo,
            onMoveCompleted: (move: Move) -> Unit,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            moves.forEach { move ->
                localRepo.add(move.to)
                localRepo.remove(move.from)

                onMoveCompleted(move)
            }
        }

        fun executeAddNewFiles(
            repo: Repo,
            onAddCompleted: (newFile: String) -> Unit,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            newFiles.forEach { newFile ->
                localRepo.add(newFile)

                onAddCompleted(newFile)
            }
        }
    }
}
