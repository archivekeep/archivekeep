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
import org.archivekeep.app.core.utils.generics.firstLoadedOrFailure
import org.archivekeep.app.core.utils.generics.sharedGlobalWhileSubscribed
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.core.operations.copyFile
import org.archivekeep.core.repo.LocalRepo
import org.archivekeep.utils.Loadable

class DefaultAddPushOperationService(
    private val repositoryService: RepositoryService,
) : AddPushOperationService {
    private val addPushOperations = singleInstanceWeakValueMap(::RepoAddPushImpl)

    val ongoingJobs = mutableSetOf<RepoAddPushJobImpl?>()

    inner class RepoAddPushJobImpl(
        private val addPush: RepoAddPushImpl,
        override val originalIndexStatus: IndexStatus,
        private val launchOptions: RepoAddPush.LaunchOptions,
    ) : RepoAddPush.Job {
        val updateMutex = Mutex()
        var addProgress = RepoAddPush.AddProgress(emptySet(), emptyMap(), false)
        var repositoryPushStatus =
            launchOptions.selectedDestinationRepositories
                .associateWith {
                    RepoAddPush.PushProgress(emptySet(), emptyMap(), false)
                }
        var finished = false

        private val _state =
            MutableStateFlow(
                RepoAddPush.LaunchedAddPushProcess(
                    originalIndexStatus,
                    launchOptions,
                    addProgress,
                    repositoryPushStatus,
                    finished,
                ),
            )

        private var job: Job? = null

        override val state = _state.asStateFlow()

        fun run() {
            ongoingJobs.add(this)

            this.job =
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        suspend fun report() {
                            _state.emit(
                                RepoAddPush.LaunchedAddPushProcess(
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
                            addPush.currentlyRunningOperationMutable.compareAndSet(this@RepoAddPushJobImpl, null)
                            ongoingJobs.remove(this@RepoAddPushJobImpl)

                            finished = true
                            report()
                        }
                    } finally {
                        addPush.currentlyRunningOperationMutable.compareAndSet(this@RepoAddPushJobImpl, null)
                        ongoingJobs.remove(this@RepoAddPushJobImpl)
                    }
                }
        }

        override fun cancel() {
            job!!.cancel(message = "Cancelled by user")
            println("Cancelled")
        }
    }

    override fun getAddPushOperation(repositoryURI: RepositoryURI) = this.addPushOperations[repositoryURI]

    inner class RepoAddPushImpl(
        val repositoryURI: RepositoryURI,
    ) : RepoAddPush {
        internal val currentlyRunningOperationMutable = MutableStateFlow<RepoAddPushJobImpl?>(null)

        val preparationFlow =
            repositoryService
                .getRepository(repositoryURI)
                .localRepoStatus
                .transform {
                    if (it is Loadable.Loaded) {
                        val indexStatus = it.value
                        emit(
                            RepoAddPush.ReadyAddPushProcess(
                                indexStatus,
                                launch = { launchOptions ->
                                    launch(indexStatus, launchOptions)
                                },
                            ),
                        )
                    }
                }

        override val currentJobFlow = currentlyRunningOperationMutable.asStateFlow()

        override val stateFlow: Flow<RepoAddPush.State> =
            currentJobFlow
                .flatMapLatest { currentlyRunningOperation ->
                    val currentJobStateFlow =
                        currentlyRunningOperation?.state?.transform {
                            emit(it as RepoAddPush.State)
                        }

                    currentJobStateFlow ?: preparationFlow
                }.sharedGlobalWhileSubscribed()

        internal fun launch(
            originalIndexStatus: IndexStatus,
            launchOptions: RepoAddPush.LaunchOptions,
        ) {
            val newOperation =
                RepoAddPushJobImpl(
                    this,
                    originalIndexStatus,
                    launchOptions,
                )
            val newCreated = currentlyRunningOperationMutable.compareAndSet(null, newOperation)

            if (!newCreated) {
                throw IllegalStateException("Already running")
            }

            newOperation.run()
        }
    }
}
