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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.files.operations.indexupdate.AddOperationProgressTracker
import org.archivekeep.files.operations.sync.copyFile
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.LoadableWithProgress
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

        private val indexUpdateProgressTracker = AddOperationProgressTracker()

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
                    indexUpdateProgressTracker.addProgressFlow.value,
                    indexUpdateProgressTracker.moveProgressFlow.value,
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
                        indexUpdateProgressTracker.addProgressFlow.value,
                        indexUpdateProgressTracker.moveProgressFlow.value,
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
                            indexUpdateProgressTracker.onMoveCompleted(it)
                        }
                    },
                )
                updateProgress {
                    indexUpdateProgressTracker.onMovesFinished()
                }

                addPreparationResult.executeAddNewFiles(
                    repo,
                    launchOptions.filesToAdd,
                    onAddCompleted = {
                        updateProgress {
                            indexUpdateProgressTracker.onAddCompleted(it)
                        }
                    },
                )
                updateProgress {
                    indexUpdateProgressTracker.onAddFinished()
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
                    val preparationFlow =
                        AddOperation(
                            subsetGlobs = listOf("."),
                            disableFilenameCheck = false,
                            disableMovesCheck = false,
                        ).prepare(accessor.value)
                            .transform {
                                when (it) {
                                    is LoadableWithProgress.Failed -> throw it.throwable
                                    is LoadableWithProgress.Loaded -> {
                                        val preparedAddOperation = it.value

                                        emit(
                                            AddAndPushOperation.ReadyAddPushProcess(
                                                preparedAddOperation,
                                                launch = { launchOptions ->
                                                    launch(preparedAddOperation, launchOptions)
                                                },
                                            ),
                                        )
                                    }
                                    LoadableWithProgress.Loading -> {}
                                    is LoadableWithProgress.LoadingProgress -> {
                                        emit(AddAndPushOperation.PreparingAddPushProcess(it.progress))
                                    }
                                }
                            }

                    emitAll(preparationFlow)
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
