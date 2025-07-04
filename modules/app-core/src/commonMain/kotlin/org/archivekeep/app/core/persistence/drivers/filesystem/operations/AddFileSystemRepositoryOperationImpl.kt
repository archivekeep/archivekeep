package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemRepositoryURIData
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.getFileSystemForPath
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.Execution
import org.archivekeep.app.core.utils.generics.perform
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.repo.files.createFilesRepo
import kotlin.io.path.Path

abstract class AddFileSystemRepositoryOperationImpl(
    val scope: CoroutineScope,
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val path: String,
    val intendedStorageType: FileSystemStorageType?,
) {
    val pathPath = Path(path)

    protected suspend fun runInitAsPlain(
        initMutableStateFlow: MutableStateFlow<Execution>,
        addMutableStateFlow: MutableStateFlow<Execution>,
        storageMarkMutableStateFlow: MutableStateFlow<Execution?>,
    ) {
        initMutableStateFlow.perform {
            createFilesRepo(pathPath)
        }

        runAdd(
            addMutableStateFlow,
            storageMarkMutableStateFlow,
        )
    }

    protected suspend fun runInitAsEncrypted(
        password: String,
        initMutableStateFlow: MutableStateFlow<Execution>,
        addMutableStateFlow: MutableStateFlow<Execution>,
        storageMarkMutableStateFlow: MutableStateFlow<Execution?>,
    ) {
        initMutableStateFlow.perform {
            EncryptedFileSystemRepository.create(pathPath, password)
        }

        runAdd(
            addMutableStateFlow,
            storageMarkMutableStateFlow,
        )
    }

    protected suspend fun runAdd(
        addMutableStateFlow: MutableStateFlow<Execution>,
        storageMarkMutableStateFlow: MutableStateFlow<Execution?>,
    ) {
        var storageURI: StorageURI? = null

        addMutableStateFlow.perform {
            addMutableStateFlow.value = Execution.InProgress

            val largest =
                fileStores.loadFreshMountPoints().let {
                    it.getFileSystemForPath(path)
                        ?: throw RuntimeException("Mount point for `$path` not found in $it")
                }

            println("PATH: $path")
            println("Largest: $largest")

            val newRepo =
                FileSystemRepositoryURIData(
                    fsUUID = largest.fsUUID,
                    pathInFS =
                        largest.fsSubPath.trimEnd('/') +
                            path.removePrefix(
                                largest.mountPath,
                            ),
                )

            println()
            println("Add: $newRepo")
            println()

            val newRepoURI = newRepo.toURI()

            registry.updateRepositories { old ->
                old + setOf(RegisteredRepository(uri = newRepoURI))
            }

            storageURI = newRepo.storageURI
        }

        runMarkStorage(storageMarkMutableStateFlow, storageURI!!)
    }

    private suspend fun runMarkStorage(
        storageMarkMutableStateFlow: MutableStateFlow<Execution?>,
        uri: StorageURI,
    ) {
        storageMarkMutableStateFlow.perform {
            registry.updateStorage(uri) {
                when (intendedStorageType) {
                    FileSystemStorageType.LOCAL -> it.copy(isLocal = true)
                    FileSystemStorageType.EXTERNAL -> it.copy(isLocal = false)
                    null -> it
                }
            }
        }
    }
}
