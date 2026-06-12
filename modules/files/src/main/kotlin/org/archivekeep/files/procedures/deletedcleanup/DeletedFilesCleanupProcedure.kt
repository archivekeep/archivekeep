package org.archivekeep.files.procedures.deletedcleanup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure

class DeletedFilesCleanupProcedure {
    data class LaunchOptions(
        val removeFilesSubsetLimit: Set<String>? = null,
    )

    fun prepare(repo: Repo): Flow<Loadable<PreparationResult>> =
        flow {
            emit(Loadable.Loading)

            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            val modifiedIndexedFiles =
                localRepo
                    .localIndex
                    .firstLoadedOrFailure()
                    .missingFiles

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
        val missingFiles: List<String>,
    ) {
        suspend fun execute(
            repo: Repo,
            removeFilesSubsetLimit: Set<String>? = null,
            vararg progressTrackers: DeletedFilesCleanupProgressTracker,
        ) {
            executeFilesRemove(repo, removeFilesSubsetLimit, *progressTrackers)
        }

        suspend fun executeFilesRemove(
            repo: Repo,
            removeFilesSubsetLimit: Set<String>? = null,
            vararg progressTrackers: DeletedFilesCleanupProgressTracker,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            missingFiles.forEach { removedFile ->
                if (removeFilesSubsetLimit != null && !removeFilesSubsetLimit.contains(removedFile)) {
                    return@forEach
                }

                // TODO: handle failures

                localRepo.remove(removedFile)

                progressTrackers.forEach { it.onFileRemoved(removedFile) }
            }
        }
    }
}
