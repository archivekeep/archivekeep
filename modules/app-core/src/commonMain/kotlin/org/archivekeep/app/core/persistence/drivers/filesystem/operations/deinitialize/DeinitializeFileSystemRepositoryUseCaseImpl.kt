package org.archivekeep.app.core.persistence.drivers.filesystem.operations.deinitialize

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.driver.filesystem.files.FileSystemRepositoryFactory
import org.archivekeep.files.driver.filesystem.files.FilesystemWorkingRepository
import org.archivekeep.utils.loading.optional.firstFinished
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class DeinitializeFileSystemRepositoryByPathUseCaseImpl : DeinitializeFileSystemRepositoryByPathUseCase {
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
                override val path: String = path

                override suspend fun runDeinitialize() {
                    filesystemWorkingRepository.deinitialize()
                    filesystemWorkingRepository.close()
                }
            }
        }

        return DeinitializeFileSystemRepositoryPreparation.DirectoryNotRepository
    }
}

class DeinitializeFileSystemRepositoryByUriUseCaseImpl(
    private val repositoryService: RepositoryService,
) : DeinitializeFileSystemRepositoryByUriUseCase {
    override suspend fun prepare(
        scope: CoroutineScope,
        repositoryURI: RepositoryURI,
    ): DeinitializeFileSystemRepositoryPreparation {
        val repository = repositoryService.getRepository(repositoryURI)
        val accessor = repository.optionalAccessorFlow.firstFinished()

        val filesystemWorkingRepository =
            accessor as? FilesystemWorkingRepository ?: return DeinitializeFileSystemRepositoryPreparation.DirectoryNotRepository

        return object : DeinitializeFileSystemRepositoryPreparation.PlainFileSystemRepository {
            override val path: String = filesystemWorkingRepository.root.toAbsolutePath().toString()

            override suspend fun runDeinitialize() {
                filesystemWorkingRepository.deinitialize()
                filesystemWorkingRepository.close()
            }
        }
    }
}
