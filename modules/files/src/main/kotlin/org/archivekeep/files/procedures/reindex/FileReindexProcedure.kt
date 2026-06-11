package org.archivekeep.files.procedures.reindex

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure

class FileReindexProcedure {
    data class LaunchOptions(
        val reindexFilesSubsetLimit: Set<String>? = null,
    )

    fun prepare(repo: Repo): Flow<Loadable<PreparationResult>> =
        flow {
            emit(Loadable.Loading)

            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            val modifiedIndexedFiles =
                localRepo
                    .localIndex
                    .firstLoadedOrFailure()
                    .modifiedIndexedFiles

            // TODO: check for not actually modified, creation of duplications, moves overwriting indexed files

            emit(
                Loadable.Loaded(
                    PreparationResult(
                        modifiedIndexedFiles,
                    ),
                ),
            )
        }.catch { e -> emit(Loadable.Failed(e)) }

    data class PreparationResult(
        val modifiedIndexedFiles: List<String>,
    ) {
        suspend fun execute(
            repo: Repo,
            reindexFilesSubsetLimit: Set<String>? = null,
            vararg progressTrackers: FileReindexProgressTracker,
        ) {
            executeFilesReindex(repo, reindexFilesSubsetLimit, *progressTrackers)
        }

        suspend fun executeFilesReindex(
            repo: Repo,
            reindexFilesSubsetLimit: Set<String>? = null,
            vararg progressTrackers: FileReindexProgressTracker,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            modifiedIndexedFiles.forEach { newFile ->
                if (reindexFilesSubsetLimit != null && !reindexFilesSubsetLimit.contains(newFile)) {
                    return@forEach
                }

                // TODO: handle failures

                localRepo.add(newFile, reindex = true)

                progressTrackers.forEach { it.onFileReindexed(newFile) }
            }
        }
    }
}
