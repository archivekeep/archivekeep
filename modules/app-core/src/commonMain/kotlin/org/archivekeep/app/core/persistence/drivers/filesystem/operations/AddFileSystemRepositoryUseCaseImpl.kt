package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.getFileSystemForPath
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.StorageMarking
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.Execution
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.generics.perform
import org.archivekeep.files.repo.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.repo.files.openFilesRepoOrNull
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

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

        val isEncryptedRepository = EncryptedFileSystemRepository.isRepository(pathPath)
        if (isEncryptedRepository) {
            // TODO: check for and emit Status.AlreadyRegistered if the case

            val storageMarking = getStorageMarking(path, intendedStorageType)

            return (
                object :
                    AddFileSystemRepositoryOperationImpl(
                        scope,
                        registry,
                        fileStores,
                        path,
                        intendedStorageType,
                    ),
                    AddFileSystemRepositoryOperation.EncryptedFileSystemRepository {
                    override val storageMarking: StorageMarking = storageMarking

                    private val unlockMutableStateFlow: MutableStateFlow<Execution> = MutableStateFlow(Execution.NotRunning)
                    private val addMutableStateFlow: MutableStateFlow<Execution> = MutableStateFlow(Execution.NotRunning)
                    private val storageMarkMutableStateFlow: MutableStateFlow<Execution?> = MutableStateFlow(null)

                    override val unlockStatus: StateFlow<Execution> = unlockMutableStateFlow.asStateFlow()
                    override val addStatus: StateFlow<Execution> = addMutableStateFlow.asStateFlow()
                    override val storageMarkStatus: StateFlow<Execution?> = storageMarkMutableStateFlow.asStateFlow()

                    override suspend fun unlock(password: String) {
                        unlockMutableStateFlow.perform {
                            EncryptedFileSystemRepository(pathPath, password)
                        }
                    }

                    override suspend fun runAddExecution(applyMarking: Boolean?) {
                        checkMarking(storageMarking, applyMarking)

                        unlockStatus.value.let {
                            if (it !is Execution.Finished || it.outcome !is ExecutionOutcome.Success) {
                                throw IllegalStateException("Not unlocked")
                            }
                        }

                        runAdd(addMutableStateFlow, storageMarkMutableStateFlow)
                    }
                }
            )
        }

        val filesRepo = openFilesRepoOrNull(pathPath)

        if (filesRepo != null) {
            // TODO: check for and emit Status.AlreadyRegistered if the case

            val storageMarking = getStorageMarking(path, intendedStorageType)

            return (
                object :
                    AddFileSystemRepositoryOperationImpl(
                        scope,
                        registry,
                        fileStores,
                        path,
                        intendedStorageType,
                    ),
                    AddFileSystemRepositoryOperation.PlainFileSystemRepository {
                    override val storageMarking: StorageMarking = storageMarking

                    private val addMutableStateFlow: MutableStateFlow<Execution> = MutableStateFlow(Execution.NotRunning)
                    private val storageMarkMutableStateFlow: MutableStateFlow<Execution?> = MutableStateFlow(null)

                    override val addStatus: StateFlow<Execution> = addMutableStateFlow.asStateFlow()
                    override val storageMarkStatus: StateFlow<Execution?> = storageMarkMutableStateFlow.asStateFlow()

                    override suspend fun runAddExecution(applyMarking: Boolean?) {
                        checkMarking(storageMarking, applyMarking)

                        runAdd(addMutableStateFlow, storageMarkMutableStateFlow)
                    }
                }
            )
        } else {
            val parentDirRepoPath =
                run {
                    var tryPath: Path? = pathPath.parent

                    while (tryPath != null) {
                        val archive = openFilesRepoOrNull(tryPath)

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
                        val storageMarking = getStorageMarking(path, intendedStorageType)

                        object :
                            AddFileSystemRepositoryOperationImpl(
                                scope,
                                registry,
                                fileStores,
                                path,
                                intendedStorageType,
                            ),
                            AddFileSystemRepositoryOperation.DirectoryNotRepository {
                            override val storageMarking: StorageMarking = storageMarking

                            private val initMutableStateFlow: MutableStateFlow<Execution> = MutableStateFlow(Execution.NotRunning)
                            private val addMutableStateFlow: MutableStateFlow<Execution> = MutableStateFlow(Execution.NotRunning)
                            private val storageMarkMutableStateFlow: MutableStateFlow<Execution?> = MutableStateFlow(null)

                            override val initStatus: StateFlow<Execution> = addMutableStateFlow.asStateFlow()
                            override val addStatus: StateFlow<Execution> = addMutableStateFlow.asStateFlow()
                            override val storageMarkStatus: StateFlow<Execution?> = storageMarkMutableStateFlow.asStateFlow()

                            override suspend fun startInitAsPlain(applyMarking: Boolean?) {
                                checkMarking(storageMarking, applyMarking)

                                runInitAsPlain(
                                    initMutableStateFlow,
                                    addMutableStateFlow,
                                    storageMarkMutableStateFlow,
                                )
                            }

                            override suspend fun startInitAsEncrypted(
                                applyMarking: Boolean?,
                                password: String,
                            ) {
                                checkMarking(storageMarking, applyMarking)

                                runInitAsEncrypted(
                                    password,
                                    initMutableStateFlow,
                                    addMutableStateFlow,
                                    storageMarkMutableStateFlow,
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    private fun checkMarking(
        storageMarking: StorageMarking,
        markConfirm: Boolean?,
    ) {
        if (storageMarking.isRemark && markConfirm != true) {
            throw IllegalArgumentException("Storage re-mark needs to be confirmed")
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
            null -> StorageMarking.ALRIGHT
        }
    }
}
