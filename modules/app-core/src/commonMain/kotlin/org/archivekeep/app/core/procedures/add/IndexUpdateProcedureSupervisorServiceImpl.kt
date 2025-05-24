package org.archivekeep.app.core.procedures.add

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.app.core.utils.AbstractJobGuardRunnable
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateStructuredProgressTracker
import org.archivekeep.files.procedures.indexupdate.IndexUpdateTextualProgressTracker
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.LoadableWithProgress
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import java.io.PrintWriter

class IndexUpdateProcedureSupervisorServiceImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
) : IndexUpdateProcedureSupervisorService {
    private val addPushOperations = singleInstanceWeakValueMap(::IndexUpdateProcedureImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, JobImpl>()

    override fun getAddOperation(repositoryURI: RepositoryURI): IndexUpdateProcedureSupervisor = addPushOperations[repositoryURI]

    inner class IndexUpdateProcedureImpl(
        val repositoryURI: RepositoryURI,
    ) : IndexUpdateProcedureSupervisor {
        override val currentJobFlow: StateFlow<JobImpl?> = jobGuards.stateHoldersWeakReference[repositoryURI].asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun prepare(): Flow<Loadable<IndexUpdateProcedureSupervisor.Preparation>> =
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
                                        is LoadableWithProgress.Failed -> Loadable.Failed(it.throwable)
                                        is LoadableWithProgress.Loaded -> {
                                            Loadable.Loaded(
                                                IndexUpdateProcedureSupervisor.Preparation(
                                                    it.value,
                                                    launch = { launchOptions ->
                                                        val job =
                                                            JobImpl(
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
                                                ),
                                            )
                                        }
                                        LoadableWithProgress.Loading -> Loadable.Loading
                                        is LoadableWithProgress.LoadingProgress -> {
                                            Loadable.Loaded(
                                                IndexUpdateProcedureSupervisor.Preparation(
                                                    it.progress,
                                                    launch = {
                                                        throw IllegalStateException("Not prepared")
                                                    },
                                                ),
                                            )
                                        }
                                    }
                                }
                        }.flowOn(Dispatchers.IO)
                }
    }

    inner class JobImpl(
        private val repositoryAccess: Repo,
        val preparationResult: IndexUpdateProcedure.PreparationResult,
        val launchOptions: IndexUpdateProcedure.LaunchOptions,
    ) : AbstractJobGuardRunnable(),
        JobWrapper<IndexUpdateProcedureSupervisor.JobState> {
        private val executionLog = SyncFlowStringWriter()
        private val writter = IndexUpdateTextualProgressTracker(PrintWriter(executionLog.writer, true))

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
                        writter,
                        indexUpdateProgressTracker,
                    )
                }
            }

        override val state: Flow<IndexUpdateProcedureSupervisor.JobState> =
            combine(
                indexUpdateProgressTracker.addProgressFlow,
                indexUpdateProgressTracker.moveProgressFlow,
                executionLog.string,
                job.executionState,
            ) { addProgress, moveProgress, log, jobState ->
                IndexUpdateProcedureSupervisor.JobState(
                    addProgress,
                    moveProgress,
                    log,
                    jobState,
                )
            }

        override suspend fun execute() {
            job.run()
        }
    }
}
