package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.getFileSystemForPath
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.StorageMarking
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.driver.filesystem.files.FilesRepo
import org.archivekeep.files.driver.filesystem.files.FilesSqliteRepo
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

internal fun checkMarking(
    storageMarking: StorageMarking,
    markConfirm: Boolean?,
) {
    if (storageMarking.isRemark && markConfirm != true) {
        throw IllegalArgumentException("Storage re-mark needs to be confirmed")
    }
}

class AddFileSystemRepositoryUseCaseImpl(
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val storageRegistry: StorageRegistry,
) : AddFileSystemRepositoryUseCase {
    override suspend fun begin(
        scope: CoroutineScope,
        path: String,
        intendedStorageType: FileSystemStorageType?,
    ): AddFileSystemRepositoryOperation {
        val pathPath = Path(path)

        if (!pathPath.exists() || !pathPath.isDirectory()) {
            return AddFileSystemRepositoryOperation.Invalid.NotExisting
        }

        // TODO: check for and emit Status.AlreadyRegistered if the case

        if (EncryptedFileSystemRepository.isRepository(pathPath)) {
            val storageMarking = getStorageMarking(path, intendedStorageType)

            return (
                AddEncryptedFileSystemRepositoryOperation(scope, registry, fileStores, path, intendedStorageType, storageMarking)
            )
        }

        if (FilesSqliteRepo.openOrNull(pathPath) != null || FilesRepo.openOrNull(pathPath) != null) {
            val storageMarking = getStorageMarking(path, intendedStorageType)

            return (
                AddPlainFileSystemRepositoryOperation(scope, registry, fileStores, path, intendedStorageType, storageMarking)
            )
        } else {
            val parentDirRepoPath =
                run {
                    var tryPath: Path? = pathPath.parent

                    while (tryPath != null) {
                        val archive = FilesRepo.openOrNull(tryPath)

                        if (archive != null) {
                            return@run tryPath
                        }

                        if (EncryptedFileSystemRepository.isRepository(tryPath)) {
                            return@run tryPath
                        }

                        tryPath = tryPath.parent
                    }

                    return@run null
                }

            return (
                run {
                    if (parentDirRepoPath != null) {
                        AddFileSystemRepositoryOperation.Invalid.NotRoot(parentDirRepoPath.toString())
                    } else {
                        AddNewFileSystemRepositoryOperation(
                            scope,
                            registry,
                            fileStores,
                            path,
                            intendedStorageType,
                            getStorageMarking(path, intendedStorageType),
                            encryptedNotPossibleDueToNotEmpty = pathPath.toFile().list().isNotEmpty(),
                        )
                    }
                }
            )
        }
    }

    private suspend fun getStorageMarking(
        path: String,
        intendedStorageType: FileSystemStorageType?,
    ): StorageMarking {
        val fs = fileStores.loadFreshMountPoints().getFileSystemForPath(path)
        val storage = fs?.let { storageRegistry.getStorageByURI(it.storageURI) }

        return when (intendedStorageType) {
            FileSystemStorageType.LOCAL -> {
                when (storage?.isLocal) {
                    null -> StorageMarking.NEEDS_MARK_AS_LOCAL
                    true -> StorageMarking.ALRIGHT
                    false -> StorageMarking.NEEDS_REMARK_AS_LOCAL
                }
            }

            FileSystemStorageType.EXTERNAL -> {
                when (storage?.isLocal) {
                    null -> StorageMarking.NEEDS_MARK_AS_EXTERNAL
                    true -> StorageMarking.NEEDS_REMARK_AS_EXTERNAL
                    false -> StorageMarking.ALRIGHT
                }
            }

            null -> {
                StorageMarking.ALRIGHT
            }
        }
    }
}
