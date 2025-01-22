package org.archivekeep.app.core.operations.derived

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapToLoadable
import org.archivekeep.app.core.utils.generics.sharedGlobalWhileSubscribed
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.core.operations.CompareOperation
import org.archivekeep.core.operations.PreparedSyncOperation
import org.archivekeep.core.operations.RelocationSyncMode
import org.archivekeep.core.operations.SyncLogger
import org.archivekeep.core.operations.SyncOperation
import org.archivekeep.core.operations.SyncPlanStep
import org.archivekeep.core.repo.Repo
import org.archivekeep.utils.Loadable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private typealias RepositoryIDPair = Pair<RepositoryURI, RepositoryURI>

class DefaultSyncService(
    private val repositoryService: RepositoryService,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val throttlePauseDuration: Duration = 500.milliseconds,
) : SyncService {
    private val repoToRepoSyncs = singleInstanceWeakValueMap(::RepoToRepoSyncImpl)

    val ongoingOperations = mutableSetOf<Operation>()

    override fun getRepoToRepoSync(
        baseURI: RepositoryURI,
        otherURI: RepositoryURI,
    ): RepoToRepoSync = repoToRepoSyncs[RepositoryIDPair(baseURI, otherURI)]

    inner class RepoToRepoSyncImpl(
        key: RepositoryIDPair,
    ) : RepoToRepoSync {
        override val fromURI = key.first
        override val otherURI = key.second

        internal val currentlyRunningOperationMutableFlow = MutableStateFlow<Operation?>(null)

        override val currentlyRunningOperationFlow = currentlyRunningOperationMutableFlow.asStateFlow()

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

        override val stateFlow =
            compareStatusFlow.mapLoadedData {
                RepoToRepoSync.State(it)
            }

        override fun prepare(relocationSyncMode: RelocationSyncMode): Flow<Loadable<SyncOperationExecution.Prepared>> {
            val baseFlow = repositoryService.getRepository(fromURI).accessorFlow
            val otherFlow = repositoryService.getRepository(otherURI).accessorFlow

            val startFlow = {
                    comparisonLoadable: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
                    preparedSync: PreparedSyncOperation,
                    base: Repo,
                    other: Repo,
                ->
                val newOperation =
                    Operation(
                        repoToRepoSync = this,
                        comparisonLoadable = comparisonLoadable,
                        preparedSyncOperation = preparedSync,
                        base = base,
                        other = other,
                    )
                val newCreated = currentlyRunningOperationMutableFlow.compareAndSet(null, newOperation)

                if (!newCreated) {
                    throw IllegalStateException("Already running")
                }

                newOperation.run()

                newOperation
            }

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
                            SyncOperationExecution.Prepared(
                                comparisonLoadable,
                                startExecution = {
                                    startFlow(
                                        comparisonLoadable,
                                        prepared,
                                        base,
                                        other,
                                    )
                                },
                                preparedSyncOperation = prepared,
                            ),
                        )
                    }.flowOn(ioDispatcher)
                        .mapToLoadable()
                }.flatMapLatest { it }

            return currentlyRunningOperationMutableFlow.flatMapLatest {
                if (it != null) {
                    // pause new preparations
                    flowOf()
                } else {
                    preparationFlow
                }.sharedGlobalWhileSubscribed()
            }
        }
    }

    inner class Operation(
        val repoToRepoSync: RepoToRepoSyncImpl,
        val comparisonLoadable: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
        val preparedSyncOperation: PreparedSyncOperation,
        val base: Repo,
        val other: Repo,
    ) {
        private var job: Job? = null

        private val _currentState = MutableStateFlow<RunningOrCompletedSync?>(null)

        val currentState = _currentState.asStateFlow()

        fun run() {
            ongoingOperations.add(this)

            this.job =
                GlobalScope.launch(ioDispatcher) {
                    var executeResult: SyncFlowStringWriter? = null

                    try {
                        executeResult = SyncFlowStringWriter()
                        val progress = MutableStateFlow(emptyList<SyncPlanStep.Progress>())

                        _currentState.value =
                            SyncOperationExecution.Running(
                                comparisonLoadable,
                                preparedSyncOperation,
                                executeResult.string,
                                progress,
                                this@Operation,
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
                                },
                            progressReport = progressReport,
                        )

                        executeResult.writer.flush()

                        _currentState.value =
                            SyncOperationExecution.Finished(
                                comparisonLoadable,
                                preparedSyncOperation,
                                executeResult.string.value,
                                success = true,
                                cancelled = false,
                            )
                    } catch (e: CancellationException) {
                        println("Sync cancelled: $e")
                        _currentState.value =
                            SyncOperationExecution.Finished(
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
                            SyncOperationExecution.Finished(
                                comparisonLoadable,
                                preparedSyncOperation,
                                executeResult?.string?.value ?: "",
                                success = false,
                                cancelled = false,
                            )
                    } finally {
                        repoToRepoSync.currentlyRunningOperationMutableFlow.compareAndSet(this@Operation, null)
                        ongoingOperations.remove(this@Operation)
                    }
                }
        }

        fun cancel() {
            job!!.cancel(message = "Cancelled by user")
            println("Cancelled")
        }
    }
}
