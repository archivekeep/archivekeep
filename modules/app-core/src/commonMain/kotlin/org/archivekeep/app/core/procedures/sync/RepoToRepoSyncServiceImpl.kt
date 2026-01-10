package org.archivekeep.app.core.procedures.sync

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.State
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.app.core.utils.AbstractJobGuardRunnable
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.discovery.DiscoveredSync
import org.archivekeep.files.procedures.sync.discovery.RelocationSyncMode
import org.archivekeep.files.procedures.sync.discovery.SyncProcedureDiscovery
import org.archivekeep.files.procedures.sync.job.observation.WriterSyncLogger
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.optional.stateIn
import org.archivekeep.utils.loading.stateIn
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

    val jobGuards = UniqueJobGuard<RepositoryIDPair, JobWrapperImpl>()

    override fun getRepoToRepoSync(
        baseURI: RepositoryURI,
        otherURI: RepositoryURI,
    ): RepoToRepoSync = repoToRepoSyncs[RepositoryIDPair(baseURI, otherURI)]

    inner class RepoToRepoSyncImpl(
        private val key: RepositoryIDPair,
    ) : RepoToRepoSync {
        val fromURI = key.first
        val otherURI = key.second

        override val currentJobFlow =
            jobGuards
                .stateHoldersWeakReference[key]
                .asStateFlow()

        val compareStatusFlow =
            combine(
                repositoryService
                    .getRepository(fromURI)
                    .indexFlowWithCaching,
                repositoryService
                    .getRepository(otherURI)
                    .indexFlowWithCaching,
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

                        is OptionalLoadable.NotAvailable -> {
                            println("Sync status not available: $fromURI -> $otherURI: ${it.cause}")
                        }

                        is OptionalLoadable.LoadedAvailable -> {}
                        OptionalLoadable.Loading -> {}
                    }
                }.flowOn(computeDispatcher)
                .stateIn(scope)

        override val compareStateFlow =
            compareStatusFlow.mapLoadedData {
                RepoToRepoSync.CompareState(it)
            }

        @OptIn(ExperimentalCoroutinesApi::class)
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
                            SyncProcedureDiscovery(relocationSyncMode).prepareFromComparison(
                                comparisonLoadable.value,
                            )

                        emit(
                            State.Prepared(
                                comparisonLoadable,
                                startExecution = { limitToSubset ->
                                    val newJob =
                                        JobWrapperImpl(
                                            discoveredSync = prepared,
                                            base = base,
                                            other = other,
                                            limitToSubset = limitToSubset,
                                        )
                                    jobGuards.launch(scope, Dispatchers.IO, this@RepoToRepoSyncImpl.key, newJob)
                                    newJob
                                },
                                discoveredSync = prepared,
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
                }.stateIn(scope)
            }
        }
    }

    class JobWrapperImpl(
        val discoveredSync: DiscoveredSync,
        val base: Repo,
        val other: Repo,
        val limitToSubset: Set<SyncOperation>,
    ) : AbstractJobGuardRunnable(),
        JobWrapper<JobState> {
        private val executionLog = SyncFlowStringWriter()
        private val executionErrorLog = SyncFlowStringWriter()

        val job =
            discoveredSync.createJob(
                base,
                other,
                prompter = { true },
                limitToSubset = limitToSubset,
                observer = WriterSyncLogger(executionLog.writer, executionErrorLog.writer),
            )

        override val state: Flow<JobState> =
            job.executionState
                .map {
                    JobState(
                        job.task.executionProgressSummaryStateFlow,
                        job.inProgressOperationsProgressFlow,
                        executionLog.string,
                        executionErrorLog.string,
                        it,
                    )
                }

        override suspend fun execute() {
            job.run()
        }
    }
}
