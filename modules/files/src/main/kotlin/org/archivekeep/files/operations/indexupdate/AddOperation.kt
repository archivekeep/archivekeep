package org.archivekeep.files.operations.indexupdate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.archivekeep.files.exceptions.InvalidFilename
import org.archivekeep.files.operations.indexupdate.AddOperation.PreparationResult.Move
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.LoadableWithProgress
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

    fun prepare(repo: Repo): Flow<LoadableWithProgress<PreparationResult, PreparationProgress>> =
        flow {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            val matchedFiles = localRepo.findAllFiles(subsetGlobs)

            var progress =
                PreparationProgress(
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyMap(),
                )

            suspend fun updateProgress(updater: (old: PreparationProgress) -> PreparationProgress) {
                progress = updater(progress)
                emit(LoadableWithProgress.LoadingProgress(progress))
            }

            val unindexedFilesMatchingPattern =
                matchedFiles.filter {
                    !localRepo.contains(it.invariantSeparatorsPathString)
                }

            updateProgress { it.copy(filesToCheck = unindexedFilesMatchingPattern.map { it.toString() }) }

            val filesWithWrongFilenames =
                unindexedFilesMatchingPattern
                    .mapNotNull {
                        it.invariantSeparatorsPathString.let { pathString ->
                            illegalCharacters
                                .firstOrNull { illegalCharacter -> pathString.contains(illegalCharacter) }
                                ?.let { illegalCharacter ->
                                    pathString to InvalidFilename(pathString, "$pathString contains illegal character $illegalCharacter")
                                }
                        }
                    }.toMap<String, Any>()

            updateProgress { it.copy(errorFiles = filesWithWrongFilenames) }

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

                        updateProgress { it.copy(moves = moves.toList()) }
                    } else {
                        unmatchedNewFiles.add(newUnindexedFile.invariantSeparatorsPathString)

                        updateProgress { it.copy(newFiles = unmatchedNewFiles.toList()) }
                    }
                }

                emit(
                    LoadableWithProgress.Loaded(
                        PreparationResult(
                            newFiles = unmatchedNewFiles.sorted(),
                            moves = moves,
                            missingFiles = remainingMissingIndexedFilesByChecksum.values.toList().sorted(),
                            errorFiles = filesWithWrongFilenames,
                        ),
                    ),
                )
            } else {
                emit(
                    LoadableWithProgress.Loaded(
                        PreparationResult(
                            unindexedFilesMatchingPattern.map { it.invariantSeparatorsPathString }.sorted(),
                            emptyList(),
                            emptyList(),
                            filesWithWrongFilenames,
                        ),
                    ),
                )
            }
        }.catch { e -> emit(LoadableWithProgress.Failed(e)) }

    sealed interface Preparation

    data class PreparationProgress(
        val filesToCheck: List<String>,
        val newFiles: List<String>,
        val moves: List<Move>,
        val errorFiles: Map<String, Any>,
    ) : Preparation {
        val checkedFiles = newFiles + moves.map { it.from }
    }

    data class PreparationResult(
        val newFiles: List<String>,
        val moves: List<Move>,
        val missingFiles: List<String>,
        val errorFiles: Map<String, Any>,
    ) : Preparation {
        data class Move(
            val from: String,
            val to: String,
        )

        suspend fun execute(
            repo: Repo,
            movesSubsetLimit: Set<Move>? = null,
            addFilesSubsetLimit: Set<String>? = null,
            vararg progressTrackers: IndexUpdateProgressTracker,
        ) {
            executeMovesReindex(repo, movesSubsetLimit, *progressTrackers)
            executeAddNewFiles(repo, addFilesSubsetLimit, *progressTrackers)
        }

        suspend fun executeMovesReindex(
            repo: Repo,
            movesSubsetLimit: Set<Move>? = null,
            vararg progressTrackers: IndexUpdateProgressTracker,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            moves.forEach { move ->
                if (movesSubsetLimit != null && !movesSubsetLimit.contains(move)) {
                    return@forEach
                }

                localRepo.add(move.to)
                localRepo.remove(move.from)

                progressTrackers.forEach { it.onMoveCompleted(move) }
            }

            progressTrackers.forEach { it.onMovesFinished() }
        }

        suspend fun executeAddNewFiles(
            repo: Repo,
            addFilesSubsetLimit: Set<String>? = null,
            vararg progressTrackers: IndexUpdateProgressTracker,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            newFiles.forEach { newFile ->
                if (addFilesSubsetLimit != null && !addFilesSubsetLimit.contains(newFile)) {
                    return@forEach
                }

                localRepo.add(newFile)

                progressTrackers.forEach { it.onAddCompleted(newFile) }
            }

            progressTrackers.forEach { it.onAddFinished() }
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
