package org.archivekeep.app.core.persistence.drivers.filesystem.operations.deinitialize

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.files.driver.filesystem.files.FileSystemRepositoryFactory
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

        val filesystemWorkingRepository = FileSystemRepositoryFactory.openPlainRepositoryOrNull(pathPath)

        if (filesystemWorkingRepository != null) {
            return object : DeinitializeFileSystemRepositoryPreparation.PlainFileSystemRepository {
                override suspend fun runDeinitialize() = filesystemWorkingRepository.deinitialize()
            }
        }

        return DeinitializeFileSystemRepositoryPreparation.DirectoryNotRepository
    }
}
