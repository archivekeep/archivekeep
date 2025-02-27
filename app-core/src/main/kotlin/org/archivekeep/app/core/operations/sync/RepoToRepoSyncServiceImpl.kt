package org.archivekeep.app.core.operations.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.State
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.sharedGlobalWhileSubscribed
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.PreparedSyncOperation
import org.archivekeep.files.operations.RelocationSyncMode
import org.archivekeep.files.operations.SyncLogger
import org.archivekeep.files.operations.SyncOperation
import org.archivekeep.files.operations.SyncPlanStep
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private typealias RepositoryIDPair = Pair<RepositoryURI, RepositoryURI>

class RepoToRepoSyncServiceImpl(
    val scope: CoroutineScope,
    private val repositoryService: RepositoryService,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val throttlePauseDuration: Duration = 500.milliseconds,
) : RepoToRepoSyncService {
    private val repoToRepoSyncs = singleInstanceWeakValueMap(::RepoToRepoSyncImpl)

    val jobGuards = UniqueJobGuard<RepositoryIDPair, JobImpl>()

    override fun getRepoToRepoSync(
        baseURI: RepositoryURI,
        otherURI: RepositoryURI,
    ): RepoToRepoSync = repoToRepoSyncs[RepositoryIDPair(baseURI, otherURI)]

    inner class RepoToRepoSyncImpl(
        private val key: RepositoryIDPair,
    ) : RepoToRepoSync {
        val fromURI = key.first
        val otherURI = key.second

        override val currentJobFlow = jobGuards.stateHoldersWeakReference[key].asStateFlow()

        val compareStatusFlow =
            combine(
                repositoryService
                    .getRepository(fromURI)
                    .indexFlow,
                repositoryService
                    .getRepository(otherURI)
                    .indexFlow,
            ) { base, other ->
                Pair(base, other)
            }.conflate()
                .transform { (baseLoadable, otherLoadable) ->
                    when (baseLoadable) {
                        is OptionalLoadable.Failed -> {
                            emit(
                                OptionalLoadable.Failed(
                                    RuntimeException(
                                        "Base index",
                                        baseLoadable.cause,
                                    ),
                                ),
                            )
                            return@transform
                        }

                        OptionalLoadable.Loading -> {
                            emit(OptionalLoadable.Loading)
                            return@transform
                        }

                        is OptionalLoadable.LoadedAvailable -> {}
                        is OptionalLoadable.NotAvailable -> {
                            emit(OptionalLoadable.NotAvailable())
                            return@transform
                        }
                    }

                    when (otherLoadable) {
                        is OptionalLoadable.Failed -> {
                            emit(
                                OptionalLoadable.Failed(
                                    RuntimeException(
                                        "Base index",
                                        otherLoadable.cause,
                                    ),
                                ),
                            )
                            return@transform
                        }

                        OptionalLoadable.Loading -> {
                            emit(OptionalLoadable.Loading)
                            return@transform
                        }

                        is OptionalLoadable.LoadedAvailable -> {}
                        is OptionalLoadable.NotAvailable -> {
                            emit(OptionalLoadable.NotAvailable())
                            return@transform
                        }
                    }

                    val base = baseLoadable.value
                    val other = otherLoadable.value

                    val op = CompareOperation()
                    val result = op.calculate(base, other)

                    println("Computed sync status: $fromURI, $otherURI")

                    emit(OptionalLoadable.LoadedAvailable(result))

                    delay(throttlePauseDuration)
                }.onEach {
                    when (it) {
                        is OptionalLoadable.Failed -> {
                            println("Sync status failed: $fromURI -> $otherURI: ${it.cause}")
                            it.cause.printStackTrace()
                        }

                        is OptionalLoadable.LoadedAvailable -> {
                            println("Sync status loaded: $fromURI -> $otherURI")
                        }

                        is OptionalLoadable.NotAvailable -> {
                            println("Sync status not available: $fromURI -> $otherURI: ${it.cause}")
                        }

                        OptionalLoadable.Loading -> {}
                    }
                }.flowOn(computeDispatcher)
                .sharedGlobalWhileSubscribed()

        override val compareStateFlow =
            compareStatusFlow.mapLoadedData {
                RepoToRepoSync.CompareState(it)
            }

        override fun prepare(relocationSyncMode: RelocationSyncMode): Flow<Loadable<State.Prepared>> {
            val baseFlow = repositoryService.getRepository(fromURI).accessorFlow
            val otherFlow = repositoryService.getRepository(otherURI).accessorFlow

            val preparationFlow =
                combine(
                    compareStatusFlow,
                    baseFlow,
                    otherFlow,
                ) { comparisonLoadable, baseLoadable, otherLoadable ->

                    println("Returning preparation")

                    when (baseLoadable) {
                        is Loadable.Failed -> {
                            return@combine flowOf(
                                Loadable.Failed(
                                    RuntimeException(
                                        "Base",
                                        baseLoadable.throwable,
                                    ),
                                ),
                            )
                        }

                        Loadable.Loading -> {
                            return@combine flowOf(Loadable.Loading)
                        }

                        is Loadable.Loaded -> {}
                    }
                    when (otherLoadable) {
                        is Loadable.Failed -> {
                            return@combine flowOf(
                                Loadable.Failed(
                                    RuntimeException(
                                        "Other",
                                        otherLoadable.throwable,
                                    ),
                                ),
                            )
                        }

                        Loadable.Loading -> {
                            return@combine flowOf(Loadable.Loading)
                        }

                        is Loadable.Loaded -> {}
                    }

                    val base = baseLoadable.value
                    val other = otherLoadable.value

                    flow {
                        if (comparisonLoadable !is OptionalLoadable.LoadedAvailable) {
                            return@flow
                        }

                        val prepared =
                            SyncOperation(relocationSyncMode).prepareFromComparison(
                                comparisonLoadable.value,
                            )

                        emit(
                            State.Prepared(
                                comparisonLoadable,
                                startExecution = {
                                    val newJob =
                                        JobImpl(
                                            comparisonLoadable = comparisonLoadable,
                                            preparedSyncOperation = prepared,
                                            base = base,
                                            other = other,
                                        )
                                    jobGuards.launch(scope, Dispatchers.IO, this@RepoToRepoSyncImpl.key, newJob)
                                    newJob
                                },
                                preparedSyncOperation = prepared,
                            ),
                        )
                    }.flowOn(ioDispatcher)
                        .mapToLoadable()
                }.flatMapLatest { it }

            return currentJobFlow.flatMapLatest {
                if (it != null) {
                    // pause new preparations
                    flowOf()
                } else {
                    preparationFlow
                }.sharedGlobalWhileSubscribed()
            }
        }
    }

    inner class JobImpl(
        val comparisonLoadable: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
        val preparedSyncOperation: PreparedSyncOperation,
        val base: Repo,
        val other: Repo,
    ) : RepoToRepoSync.Job,
        UniqueJobGuard.RunnableJob {
        private var job: Job? = null

        private val _currentState = MutableStateFlow<JobState?>(null)

        override val currentState = _currentState.asStateFlow()

        override suspend fun run(job: Job) {
            this.job = job

            var executeResult: SyncFlowStringWriter? = null

            try {
                executeResult = SyncFlowStringWriter()
                val progress = MutableStateFlow(emptyList<SyncPlanStep.Progress>())

                _currentState.value =
                    JobState.Running(
                        comparisonLoadable,
                        preparedSyncOperation,
                        executeResult.string,
                        progress,
                        this@JobImpl,
                    )

                val progressReport = { newProgress: List<SyncPlanStep.Progress> ->
                    progress.value = newProgress
                }

                preparedSyncOperation.execute(
                    base,
                    other,
                    prompter = { true },
                    logger =
                        object : SyncLogger {
                            override fun onFileStored(filename: String) {
                                executeResult.writer.println("file stored: $filename")
                                executeResult.writer.flush()
                            }

                            override fun onFileMoved(
                                from: String,
                                to: String,
                            ) {
                                executeResult.writer.println("file moved: $from -> $to")
                                executeResult.writer.flush()
                            }

                            override fun onFileDeleted(filename: String) {
                                executeResult.writer.println("file deleted: $filename")
                                executeResult.writer.flush()
                            }
                        },
                    progressReport = progressReport,
                )

                executeResult.writer.flush()

                _currentState.value =
                    JobState.Finished(
                        comparisonLoadable,
                        preparedSyncOperation,
                        executeResult.string.value,
                        success = true,
                        cancelled = false,
                    )
            } catch (e: CancellationException) {
                println("Sync cancelled: $e")
                _currentState.value =
                    JobState.Finished(
                        comparisonLoadable,
                        preparedSyncOperation,
                        executeResult?.string?.value ?: "",
                        success = true,
                        cancelled = true,
                    )
            } catch (e: Throwable) {
                println("Sync failed: $e")
                e.printStackTrace()
                _currentState.value =
                    JobState.Finished(
                        comparisonLoadable,
                        preparedSyncOperation,
                        executeResult?.string?.value ?: "",
                        success = false,
                        cancelled = false,
                    )
            }
        }

        override fun cancel() {
            job!!.cancel(message = "Cancelled by user")
            println("Cancelled")
        }
    }
}
