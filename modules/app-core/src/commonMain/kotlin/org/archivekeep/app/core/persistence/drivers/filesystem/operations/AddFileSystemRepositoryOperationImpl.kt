package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemRepositoryURIData
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.getFileSystemForPath
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.AddStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.InitStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.PreparationStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.StorageMarkStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation.StorageMarking
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.files.createFilesRepo
import org.archivekeep.files.repo.files.openFilesRepoOrNull
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class AddFileSystemRepositoryOperationImpl(
    val scope: CoroutineScope,
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val storageRegistry: StorageRegistry,
    val path: String,
    val intendedStorageType: FileSystemStorageType?,
) : AddFileSystemRepositoryOperation {
    private val preparationMutableStateFlow =
        MutableStateFlow<PreparationStatus>(PreparationStatus.Preparing)
    private val initMutableStateFlow: MutableStateFlow<InitStatus?> = MutableStateFlow(null)
    private val addMutableStateFlow: MutableStateFlow<AddStatus?> = MutableStateFlow(null)
    private val storageMarkMutableStateFlow: MutableStateFlow<StorageMarkStatus?> = MutableStateFlow(null)
    private val completedMutableStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val preparationStatus = preparationMutableStateFlow.asStateFlow()
    override val initStatus = initMutableStateFlow.asStateFlow()
    override val addStatus = addMutableStateFlow.asStateFlow()
    override val storageMarkStatus = storageMarkMutableStateFlow.asStateFlow()
    override val completed = completedMutableStateFlow.asStateFlow()

    init {
        launchPrepare()
    }

    private var launchedPreparation: Job? = null
    private var launchedInit: Job? = null
    private var launchedAdd: Job? = null
    private var launchedMark: Job? = null

    val pathPath = Path(path)

    override fun cancel() {
        launchedPreparation?.cancel()
        launchedAdd?.cancel()
        launchedInit?.cancel()
        launchedMark?.cancel()
    }

    fun launchPrepare() {
        scope.launch {
            try {
                prepare()
            } catch (e: Throwable) {
                preparationMutableStateFlow.value = PreparationStatus.PreparationException(e)
            }
        }
    }

    private suspend fun prepare() {
        if (!pathPath.exists() || !pathPath.isDirectory()) {
            preparationMutableStateFlow.value = (PreparationStatus.NotExisting)
            return
        }

        val filesRepo = openFilesRepoOrNull(pathPath)

        if (filesRepo != null) {
            // TODO: check for and emit Status.AlreadyRegistered if the case

            val storageMarking = getStorageMarking(path)

            preparationMutableStateFlow.value = (
                PreparationStatus.ReadyForAdd(
                    startAddExecution = { markConfirm ->
                        checkMarking(storageMarking, markConfirm)
                        launchAdd()
                    },
                    storageMarking = storageMarking,
                )
            )

            return
        } else {
            val parentDirRepo =
                run {
                    var tryPath: Path? = pathPath.parent

                    while (tryPath != null) {
                        val archive = openFilesRepoOrNull(tryPath)

                        if (archive != null) {
                            return@run archive
                        }

                        tryPath = tryPath.parent
                    }

                    return@run null
                }

            val result =
                run {
                    if (parentDirRepo != null) {
                        PreparationStatus.NotRoot(
                            parentDirRepo.root.toString(),
                        )
                    } else {
                        val storageMarking = getStorageMarking(path)

                        PreparationStatus.ReadyForInit(
                            startInit = { markConfirm ->
                                checkMarking(storageMarking, markConfirm)
                                launchInit()
                            },
                            storageMarking = storageMarking,
                        )
                    }
                }
            preparationMutableStateFlow.value = result
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

    private suspend fun getStorageMarking(path: String): StorageMarking {
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

    private fun launchInit() {
        if (launchedAdd != null) {
            throw RuntimeException("Already initializing")
        }

        initMutableStateFlow.value = InitStatus.Initializing

        launchedInit =
            scope.launch {
                try {
                    createFilesRepo(pathPath)

                    initMutableStateFlow.value = (InitStatus.InitSuccessful)
                } catch (e: Throwable) {
                    initMutableStateFlow.value = (
                        InitStatus.InitFailed(
                            e.message ?: e.toString(),
                            e,
                        )
                    )
                    return@launch
                }

                launchAdd()
            }
    }

    private fun launchAdd() {
        if (launchedAdd != null) {
            throw RuntimeException("Already launched")
        }

        addMutableStateFlow.value = AddStatus.Adding

        launchedAdd =
            scope.launch {
                try {
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

                    val newRepoURI = RepositoryURI("filesystem", newRepo.serialized())

                    registry.updateRepositories { old ->
                        old + setOf(RegisteredRepository(uri = newRepoURI))
                    }

                    addMutableStateFlow.value = (AddStatus.AddSuccessful)

                    if (false) {
                        completedMutableStateFlow.value = true
                    } else {
                        launchMarkStorage(newRepo.storageURI)
                    }
                } catch (e: Throwable) {
                    addMutableStateFlow.value = (
                        AddStatus.AddFailed(
                            e.message ?: e.toString(),
                            e,
                        )
                    )
                    completedMutableStateFlow.value = true
                }
            }
    }

    private fun launchMarkStorage(uri: StorageURI) {
        storageMarkMutableStateFlow.value = StorageMarkStatus.Marking

        launchedMark =
            scope.launch {
                try {
                    registry.updateStorage(uri) {
                        when (intendedStorageType) {
                            FileSystemStorageType.LOCAL -> it.copy(isLocal = true)
                            FileSystemStorageType.EXTERNAL -> it.copy(isLocal = false)
                            null -> it
                        }
                    }
                    storageMarkMutableStateFlow.value = StorageMarkStatus.Successful
                } catch (e: Throwable) {
                    storageMarkMutableStateFlow.value = StorageMarkStatus.Failed(e.toString(), e)
                } finally {
                    completedMutableStateFlow.value = true
                }
            }
    }
}
