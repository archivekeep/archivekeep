package org.archivekeep.files.operations

import org.archivekeep.files.operations.AddOperation.PreparationResult.Move
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.Repo
import java.io.PrintWriter
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
    data class LaunchOptions(
        val addFilesSubsetLimit: Set<String>? = null,
        val movesSubsetLimit: Set<Move>? = null,
    )

    suspend fun prepare(repo: Repo): PreparationResult {
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

        suspend fun executeMovesReindex(
            repo: Repo,
            movesSubsetLimit: Set<Move>? = null,
            onMoveCompleted: suspend (move: Move) -> Unit,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            moves.forEach { move ->
                if (movesSubsetLimit != null && !movesSubsetLimit.contains(move)) {
                    return@forEach
                }

                localRepo.add(move.to)
                localRepo.remove(move.from)

                onMoveCompleted(move)
            }
        }

        suspend fun executeAddNewFiles(
            repo: Repo,
            addFilesSubsetLimit: Set<String>? = null,
            onAddCompleted: suspend (newFile: String) -> Unit,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            newFiles.forEach { newFile ->
                if (addFilesSubsetLimit != null && !addFilesSubsetLimit.contains(newFile)) {
                    return@forEach
                }

                localRepo.add(newFile)

                onAddCompleted(newFile)
            }
        }

        fun printSummary(
            out: PrintWriter,
            indent: String = "\t",
            transformPath: (path: String) -> String = { it },
        ) {
            if (missingFiles.isNotEmpty()) {
                out.println("Missing indexed files not matched by add:")
                missingFiles.forEach { out.println("${indent}${transformPath(it)}") }
                out.println()
            }

            out.println("New files to be indexed:")
            newFiles.forEach { out.println("${indent}${transformPath(it)}") }

            if (moves.isNotEmpty()) {
                out.println()
                out.println("Files to be moved:")
                moves.forEach {
                    out.println(
                        "${indent}${transformPath(it.from)} -> ${transformPath(it.to)}",
                    )
                }
            }
        }
    }
}
