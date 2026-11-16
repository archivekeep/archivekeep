package org.archivekeep.files.procedures.verify

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.procedures.indexupdate.RepoPerformanceTracker
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure
import java.io.PrintWriter

class VerifyContentsProcedure {
    fun prepare(repo: Repo): Flow<Loadable<PreparationResult>> =
        flow {
            emit(Loadable.Loading)

            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")

            val indexedFiles =
                localRepo
                    .indexFlow
                    .firstLoadedOrFailure()
                    .files
                    .map { it.path }

            emit(
                Loadable.Loaded(
                    PreparationResult(
                        indexedFiles,
                    ),
                ),
            )
        }.catch { e -> emit(Loadable.Failed(e)) }

    data class PreparationResult(
        val filesToVerify: List<String>,
    ) {
        suspend fun execute(
            repo: Repo,
            out: PrintWriter? = null,
            vararg progressTrackers: VerifyProgressTracker,
        ) {
            val localRepo = repo as? LocalRepo ?: throw RuntimeException("not local repo")
            val performanceTracker = RepoPerformanceTracker.DefaultImpl()

            try {
                filesToVerify.forEach { file ->
                    val indexedFile =
                        repo.indexFlow.firstLoadedOrFailure().byPath[file]
                            ?: throw RuntimeException("file not in index: $file")

                    val exists = localRepo.verifyFileExists(file)
                    if (!exists) {
                        progressTrackers.forEach { it.onFileVerificationFailed(file, "File not found") }
                        return@forEach
                    }

                    val actualChecksum = localRepo.computeFileChecksum(file, performanceTracker)
                    if (actualChecksum != indexedFile.checksumSha256) {
                        progressTrackers.forEach {
                            it.onFileVerificationFailed(file, "Checksum mismatch: expected=${indexedFile.checksumSha256}, actual=$actualChecksum")
                        }
                    } else {
                        progressTrackers.forEach { it.onFileVerified(file) }
                    }
                }
            } finally {
                performanceTracker.dump(out)
            }
        }
    }
}
