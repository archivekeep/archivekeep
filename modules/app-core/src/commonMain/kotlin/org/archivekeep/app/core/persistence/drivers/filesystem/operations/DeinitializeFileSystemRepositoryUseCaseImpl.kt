package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.files.repo.files.openFilesRepoOrNull
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class DeinitializeFileSystemRepositoryUseCaseImpl : DeinitializeFileSystemRepositoryUseCase {
    override suspend fun prepare(
        scope: CoroutineScope,
        path: String,
    ): DeinitializeFileSystemRepositoryPreparation {
        val pathPath = Path(path)

        if (!pathPath.exists() || !pathPath.isDirectory()) {
            return DeinitializeFileSystemRepositoryPreparation.DirectoryNotRepository
        }

        val filesRepo = openFilesRepoOrNull(pathPath)

        if (filesRepo != null) {
            return object : DeinitializeFileSystemRepositoryPreparation.PlainFileSystemRepository {
                override suspend fun runDeinitialize() = filesRepo.deinitialize()
            }
        }

        return DeinitializeFileSystemRepositoryPreparation.DirectoryNotRepository
    }
}
