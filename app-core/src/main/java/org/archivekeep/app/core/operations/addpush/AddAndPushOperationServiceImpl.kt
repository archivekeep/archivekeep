package org.archivekeep.app.core.operations.addpush

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.operations.derived.IndexStatus
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.firstLoadedOrFailure
import org.archivekeep.app.core.utils.generics.sharedGlobalWhileSubscribed
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.core.operations.copyFile
import org.archivekeep.core.repo.LocalRepo
import org.archivekeep.utils.Loadable

class AddAndPushOperationServiceImpl(
    private val repositoryService: RepositoryService,
) : AddAndPushOperationService {
    private val addPushOperations = singleInstanceWeakValueMap(::AddAndPushOperationImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, RepoAddPushJobImpl>()

    inner class RepoAddPushJobImpl(
        private val addPush: AddAndPushOperationImpl,
        override val originalIndexStatus: IndexStatus,
        private val launchOptions: AddAndPushOperation.LaunchOptions,
    ) : AddAndPushOperation.Job,
        UniqueJobGuard.RunnableJob {
        val updateMutex = Mutex()
        var addProgress = AddAndPushOperation.AddProgress(emptySet(), emptyMap(), false)
        var repositoryPushStatus =
            launchOptions.selectedDestinationRepositories
                .associateWith {
                    AddAndPushOperation.PushProgress(emptySet(), emptyMap(), false)
                }
        var finished = false

        private val _state =
            MutableStateFlow(
                AddAndPushOperation.LaunchedAddPushProcess(
                    originalIndexStatus,
                    launchOptions,
                    addProgress,
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
                        originalIndexStatus,
                        launchOptions,
                        addProgress,
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

                println("Add: ${launchOptions.selectedFiles}")

                launchOptions.selectedFiles.forEach { fileToAdd ->
                    println("add: $fileToAdd")
                    try {
                        repo.add(fileToAdd)
                        updateProgress {
                            addProgress =
                                addProgress.copy(
                                    added = addProgress.added + listOf(fileToAdd),
                                )
                        }
                    } catch (e: Exception) {
                        updateProgress {
                            addProgress =
                                addProgress.copy(
                                    error = addProgress.error + mapOf(fileToAdd to e.toString()),
                                )
                        }
                    }
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

                                launchOptions.selectedFiles.forEach { fileToPush ->
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
        val preparationFlow =
            repositoryService
                .getRepository(repositoryURI)
                .localRepoStatus
                .transform {
                    if (it is Loadable.Loaded) {
                        val indexStatus = it.value
                        emit(
                            AddAndPushOperation.ReadyAddPushProcess(
                                indexStatus,
                                launch = { launchOptions ->
                                    launch(indexStatus, launchOptions)
                                },
                            ),
                        )
                    }
                }

        override val currentJobFlow = jobGuards.stateHoldersWeakReference[repositoryURI]

        override val stateFlow: Flow<AddAndPushOperation.State> =
            currentJobFlow
                .flatMapLatest { currentlyRunningOperation ->
                    val currentJobStateFlow =
                        currentlyRunningOperation?.state?.transform {
                            emit(it as AddAndPushOperation.State)
                        }

                    currentJobStateFlow ?: preparationFlow
                }.sharedGlobalWhileSubscribed()

        internal fun launch(
            originalIndexStatus: IndexStatus,
            launchOptions: AddAndPushOperation.LaunchOptions,
        ) {
            val newOperation =
                RepoAddPushJobImpl(
                    this,
                    originalIndexStatus,
                    launchOptions,
                )

            jobGuards.launch(
                GlobalScope,
                Dispatchers.IO,
                repositoryURI,
                newOperation,
            )
        }
    }
}
