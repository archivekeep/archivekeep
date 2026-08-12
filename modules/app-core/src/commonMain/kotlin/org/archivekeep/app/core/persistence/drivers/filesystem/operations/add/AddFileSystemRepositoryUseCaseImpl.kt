package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.getFileSystemForPath
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.driver.filesystem.files.FilesRepo
import org.archivekeep.files.driver.filesystem.files.FilesSqliteRepo
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

internal fun checkInput(
    storageState: StorageRegistrationState,
    storageRegistrationInput: StorageRegistrationInput?,
) {
    if (storageState is StorageRegistrationState.Unregistered && storageRegistrationInput == null) {
        throw IllegalArgumentException("Storage re-mark needs to be confirmed")
    }
}

class AddFileSystemRepositoryUseCaseImpl(
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val storageRegistry: StorageRegistry,
) : AddFileSystemRepositoryUseCase {
    override suspend fun create(
        scope: CoroutineScope,
        path: String,
    ): AddFileSystemRepositoryOperation {
        val pathPath = Path(path)

        if (!pathPath.exists() || !pathPath.isDirectory()) {
            return AddFileSystemRepositoryOperation.Invalid.NotExisting
        }

        // TODO: check for and emit Status.AlreadyRegistered if the case

        if (EncryptedFileSystemRepository.isRepository(pathPath)) {
            val storageMarking = getStorageState(path)

            return (
                AddEncryptedFileSystemRepositoryOperation(scope, registry, fileStores, path, storageMarking)
            )
        }

        if (FilesSqliteRepo.isRepo(pathPath) || FilesRepo.openOrNull(pathPath) != null) {
            val storageMarking = getStorageState(path)

            return (
                AddPlainFileSystemRepositoryOperation(scope, registry, fileStores, path, storageMarking)
            )
        } else {
            val parentDirRepoPath =
                run {
                    var tryPath: Path? = pathPath.parent

                    while (tryPath != null) {
                        val archive = FilesSqliteRepo.openOrNull(tryPath) ?: FilesRepo.openOrNull(tryPath)

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
                            getStorageState(path),
                            encryptedNotPossibleDueToNotEmpty = pathPath.toFile().list().isNotEmpty(),
                        )
                    }
                }
            )
        }
    }

    private suspend fun getStorageState(path: String): StorageRegistrationState {
        val fs = fileStores.loadFreshMountPoints().getFileSystemForPath(path) ?: throw RuntimeException("No FS mounted for $path")
        val storage = fs.let { storageRegistry.getStorageByURI(it.storageURI) }

        if (storage != null) {
            return StorageRegistrationState.Registered(
                isLocal = storage.isLocal ?: true,
                label = storage.label ?: "",
            )
        } else {
            val mp = fs.mountPath

            val suggestedStorageType =
                when {
                    mp == "/" -> FileSystemStorageType.LOCAL
                    mp == "/var" -> FileSystemStorageType.LOCAL
                    mp == "/var/home" -> FileSystemStorageType.LOCAL
                    mp.startsWith("/media") -> FileSystemStorageType.EXTERNAL
                    mp.startsWith("/run/media") -> FileSystemStorageType.EXTERNAL
                    else -> null
                }

            val suggestedLabel =
                when (suggestedStorageType) {
                    FileSystemStorageType.LOCAL -> "Local"
                    else -> mp.trimEnd('/').substringAfterLast("/")
                }

            return StorageRegistrationState.Unregistered(
                suggestedType = suggestedStorageType,
                suggestedLabel =
                suggestedLabel,
            )
        }
    }
}
