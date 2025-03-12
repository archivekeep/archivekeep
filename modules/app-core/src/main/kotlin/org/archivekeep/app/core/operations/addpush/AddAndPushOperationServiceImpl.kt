package org.archivekeep.app.core.operations.addpush

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.files.operations.sync.copyFile
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure

class AddAndPushOperationServiceImpl(
    private val scope: CoroutineScope,
    private val repositoryService: RepositoryService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AddAndPushOperationService {
    private val addPushOperations = singleInstanceWeakValueMap(::AddAndPushOperationImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, RepoAddPushJobImpl>()

    inner class RepoAddPushJobImpl(
        private val addPush: AddAndPushOperationImpl,
        override val addPreparationResult: AddOperation.PreparationResult,
        private val launchOptions: AddAndPushOperation.LaunchOptions,
    ) : AddAndPushOperation.Job,
        UniqueJobGuard.RunnableJob {
        val updateMutex = Mutex()
        var addProgress = AddAndPushOperation.AddProgress(emptySet(), emptyMap(), false)
        var moveProgress = AddAndPushOperation.MoveProgress(emptySet(), emptyMap(), false)
        var repositoryPushStatus =
            launchOptions.selectedDestinationRepositories
                .associateWith {
                    AddAndPushOperation.PushProgress(emptySet(), emptySet(), emptyMap(), false)
                }
        var finished = false

        private val _state =
            MutableStateFlow(
                AddAndPushOperation.LaunchedAddPushProcess(
                    addPreparationResult,
                    launchOptions,
                    addProgress,
                    moveProgress,
                    repositoryPushStatus,
                    finished,
                ),
            )

        private var job: Job? = null

        override val state = _state.asStateFlow()

        override suspend fun run(job: Job) {
            this.job = job

            suspend fun report() {
                _state.emit(
                    AddAndPushOperation.LaunchedAddPushProcess(
                        addPreparationResult,
                        launchOptions,
                        addProgress,
                        moveProgress,
                        repositoryPushStatus,
                        finished,
                    ),
                )
            }

            suspend fun updateProgress(updater: () -> Unit) {
                updateMutex.withLock {
                    updater()
                    report()
                }
            }

            report()

            try {
                val repo =
                    repositoryService
                        .getRepository(addPush.repositoryURI)
                        .accessorFlow
                        .firstLoadedOrFailure()

                if (repo !is LocalRepo) {
                    throw RuntimeException("not local repo: ${addPush.repositoryURI}")
                }

                addPreparationResult.executeMovesReindex(
                    repo,
                    launchOptions.movesToExecute,
                    onMoveCompleted = {
                        updateProgress {
                            moveProgress =
                                moveProgress.copy(
                                    moved = moveProgress.moved + listOf(it),
                                )
                        }
                    },
                )
                updateProgress {
                    moveProgress =
                        moveProgress.copy(
                            finished = true,
                        )
                }

                addPreparationResult.executeAddNewFiles(
                    repo,
                    launchOptions.filesToAdd,
                    onAddCompleted = {
                        updateProgress {
                            addProgress =
                                addProgress.copy(
                                    added = addProgress.added + listOf(it),
                                )
                        }
                    },
                )
                updateProgress {
                    addProgress =
                        addProgress.copy(
                            finished = true,
                        )
                }

                coroutineScope {
                    launchOptions
                        .selectedDestinationRepositories
                        .map { destinationRepoID ->
                            launch {
                                println("launched: $destinationRepoID")

                                val destinationRepo =
                                    repositoryService
                                        .getRepository(destinationRepoID)
                                        .accessorFlow
                                        .firstLoadedOrFailure()

                                launchOptions.movesToExecute.forEach { move ->
                                    destinationRepo.move(move.from, move.to)

                                    updateProgress {
                                        repositoryPushStatus =
                                            repositoryPushStatus.mapValues { (k, v) ->
                                                if (k == destinationRepoID) {
                                                    v.copy(
                                                        moved = v.moved + setOf(move),
                                                    )
                                                } else {
                                                    v
                                                }
                                            }
                                    }
                                }

                                launchOptions.filesToAdd.forEach { fileToPush ->
                                    copyFile(
                                        dst = destinationRepo,
                                        base = repo,
                                        filename = fileToPush,
                                    )

                                    println("copied: $destinationRepoID - $fileToPush")

                                    updateProgress {
                                        repositoryPushStatus =
                                            repositoryPushStatus.mapValues { (k, v) ->
                                                if (k == destinationRepoID) {
                                                    v.copy(
                                                        added = v.added + setOf(fileToPush),
                                                    )
                                                } else {
                                                    v
                                                }
                                            }
                                    }
                                }
                            }
                        }.joinAll()
                }
            } finally {
                finished = true
                report()
            }
        }

        override fun cancel() {
            job!!.cancel(message = "Cancelled by user")
            println("Cancelled")
        }
    }

    override fun getAddPushOperation(repositoryURI: RepositoryURI) = this.addPushOperations[repositoryURI]

    inner class AddAndPushOperationImpl(
        val repositoryURI: RepositoryURI,
    ) : AddAndPushOperation {
        val repository = repositoryService.getRepository(repositoryURI)

        override val currentJobFlow = jobGuards.stateHoldersWeakReference[repositoryURI]

        override fun prepare(): Flow<AddAndPushOperation.State> =
            combineTransform(
                repository.accessorFlow,
                repository.localRepoStatus,
                repository.indexFlow,
            ) { accessor, _, _ ->
                // TODO: reuse already computed index and local repo status

                if (accessor is Loadable.Loaded) {
                    val preparedAddOperation =
                        AddOperation(
                            subsetGlobs = listOf("."),
                            disableFilenameCheck = false,
                            disableMovesCheck = false,
                        ).prepare(accessor.value)

                    emit(
                        AddAndPushOperation.ReadyAddPushProcess(
                            preparedAddOperation,
                            launch = { launchOptions ->
                                launch(preparedAddOperation, launchOptions)
                            },
                        ),
                    )
                }
            }.flowOn(ioDispatcher)

        internal fun launch(
            addPreparationResult: AddOperation.PreparationResult,
            launchOptions: AddAndPushOperation.LaunchOptions,
        ) {
            val newOperation =
                RepoAddPushJobImpl(
                    this,
                    addPreparationResult,
                    launchOptions,
                )

            jobGuards.launch(
                scope,
                ioDispatcher,
                repositoryURI,
                newOperation,
            )
        }
    }
}
