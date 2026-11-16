package org.archivekeep.app.core.procedures.add

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.app.core.utils.AbstractJobGuardRunnable
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.SyncFlowTextLinesWriter
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.performance.RepoPerformanceTracker
import org.archivekeep.files.procedures.indexupdate.ImmediateIndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.ImmediateIndexUpdateStructuredProgressTracker
import org.archivekeep.files.procedures.indexupdate.ImmediateIndexUpdateTextualProgressTracker
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateStructuredProgressTracker
import org.archivekeep.files.procedures.indexupdate.IndexUpdateTextualProgressTracker
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.LoadableWithProgress
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import java.io.PrintWriter

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(CoreApplicationServiceScope::class)
class IndexUpdateProcedureSupervisorServiceImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
) : IndexUpdateProcedureSupervisorService {
    private val addPushOperations = singleInstanceWeakValueMap(::IndexUpdateProcedureImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, CurrentJobWrapper<*>>()

    override fun getAddOperation(repositoryURI: RepositoryURI): IndexUpdateProcedureSupervisor = addPushOperations[repositoryURI]

    inner class IndexUpdateProcedureImpl(
        val repositoryURI: RepositoryURI,
    ) : IndexUpdateProcedureSupervisor {
        override val currentJobFlow = jobGuards.stateHoldersWeakReference[repositoryURI].asStateFlow()

        override fun cancel() {
            currentJobFlow.value?.cancel()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun discover(): Flow<Loadable<IndexUpdateProcedureSupervisor.Discovery>> =
            repositoryService
                .getRepository(repositoryURI)
                .accessorFlow
                .flatMapLoadableFlow { repositoryAccess ->
                    repositoryAccess.indexFlow
                        .flatMapLoadableFlow {
                            IndexUpdateProcedure(
                                subsetGlobs = listOf("."),
                                disableFilenameCheck = false,
                                disableMovesCheck = false,
                            ).prepare(repositoryAccess)
                                .map {
                                    when (it) {
                                        is LoadableWithProgress.Failed -> {
                                            Loadable.Failed(it.throwable)
                                        }

                                        is LoadableWithProgress.Loaded -> {
                                            Loadable.Loaded(
                                                IndexUpdateProcedureSupervisor.Discovery(
                                                    it.value,
                                                    launch = { launchOptions ->
                                                        val job =
                                                            PreparedAddJobImpl(
                                                                repositoryAccess,
                                                                it.value,
                                                                launchOptions,
                                                            )

                                                        jobGuards.launch(
                                                            scope,
                                                            Dispatchers.IO,
                                                            repositoryURI,
                                                            job,
                                                        )
                                                    },
                                                    startImmediate = {
                                                        throw IllegalStateException("Should not be called")
                                                    },
                                                ),
                                            )
                                        }

                                        LoadableWithProgress.Loading -> {
                                            Loadable.Loading
                                        }

                                        is LoadableWithProgress.LoadingProgress -> {
                                            Loadable.Loaded(
                                                IndexUpdateProcedureSupervisor.Discovery(
                                                    it.progress,
                                                    launch = {
                                                        throw IllegalStateException("Not prepared")
                                                    },
                                                    startImmediate = {
                                                        val job =
                                                            ImmediateAddJobImpl(
                                                                repositoryAccess,
                                                                it.progress.filesToCheck.toSet(),
                                                            )

                                                        jobGuards.launch(
                                                            scope,
                                                            Dispatchers.IO,
                                                            repositoryURI,
                                                            job,
                                                        )
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                }
                        }.flowOn(Dispatchers.IO)
                }
    }

    interface CurrentJobWrapper<out T : IndexUpdateProcedureSupervisor.JobState> :
        JobWrapper<T>,
        UniqueJobGuard.RunnableJob

    class PreparedAddJobImpl(
        private val repositoryAccess: Repo,
        val preparationResult: IndexUpdateProcedure.DiscoveryResult,
        val launchOptions: IndexUpdateProcedure.LaunchOptions,
    ) : AbstractJobGuardRunnable(),
        CurrentJobWrapper<IndexUpdateProcedureSupervisor.PreparedAddJobState> {
        private val executionLog = SyncFlowTextLinesWriter()
        private val writer = IndexUpdateTextualProgressTracker(PrintWriter(executionLog.writer, true))
        private val performanceTracker = RepoPerformanceTracker.DefaultImpl()

        // TODO - this should be encapsulated into procedure's logic, not supervisor
        private val movesToExecute = (launchOptions.movesSubsetLimit?.intersect(preparationResult.moves.toSet()) ?: preparationResult.moves).toSet()
        private val filesToAdd =
            (
                launchOptions.addFilesSubsetLimit?.intersect(
                    preparationResult.newFileNames.toSet(),
                ) ?: preparationResult.newFileNames
            ).toSet()

        private val indexUpdateProgressTracker =
            IndexUpdateStructuredProgressTracker(
                filesToAdd,
                movesToExecute,
            )

        val job =
            object : AbstractProcedureJob() {
                override suspend fun execute(context: ProcedureExecutionContext) {
                    preparationResult.execute(
                        repositoryAccess,
                        launchOptions.movesSubsetLimit,
                        launchOptions.addFilesSubsetLimit,
                        writer.out,
                        performanceTracker,
                        writer,
                        indexUpdateProgressTracker,
                    )
                }
            }

        override val state: Flow<IndexUpdateProcedureSupervisor.PreparedAddJobState> =
            combine(
                indexUpdateProgressTracker.addProgressFlow,
                indexUpdateProgressTracker.moveProgressFlow,
                job.executionState,
            ) { addProgress, moveProgress, jobState ->
                IndexUpdateProcedureSupervisor.PreparedAddJobState(
                    addProgress,
                    moveProgress,
                    executionLog.lines,
                    jobState,
                    performanceTracker,
                )
            }

        override suspend fun execute() {
            try {
                job.run()
            } finally {
                performanceTracker.dump(executionLog.writer)
            }
        }
    }

    class ImmediateAddJobImpl(
        private val repositoryAccess: Repo,
        val filesToProcess: Set<String>,
    ) : AbstractJobGuardRunnable(),
        CurrentJobWrapper<IndexUpdateProcedureSupervisor.ImmediateAddJobState> {
        private val executionLog = SyncFlowTextLinesWriter()
        private val writer = ImmediateIndexUpdateTextualProgressTracker(PrintWriter(executionLog.writer, true))
        private val performanceTracker = RepoPerformanceTracker.DefaultImpl()

        // TODO - this should be encapsulated into procedure's logic, not supervisor
        private val indexUpdateProgressTracker =
            ImmediateIndexUpdateStructuredProgressTracker(
                filesToProcess,
            )

        val job =
            object : AbstractProcedureJob() {
                override suspend fun execute(context: ProcedureExecutionContext) {
                    ImmediateIndexUpdateProcedure().execute(
                        repositoryAccess,
                        filesToProcess,
                        writer.out,
                        performanceTracker,
                        writer,
                        indexUpdateProgressTracker,
                    )
                }
            }

        override val state: Flow<IndexUpdateProcedureSupervisor.ImmediateAddJobState> =
            combine(
                indexUpdateProgressTracker.progressFlow,
                job.executionState,
            ) { progress, jobState ->
                IndexUpdateProcedureSupervisor.ImmediateAddJobState(
                    progress,
                    executionLog.lines,
                    jobState,
                    performanceTracker,
                )
            }

        override suspend fun execute() {
            try {
                job.run()
            } finally {
                performanceTracker.dump(executionLog.writer)
            }
        }
    }
}
