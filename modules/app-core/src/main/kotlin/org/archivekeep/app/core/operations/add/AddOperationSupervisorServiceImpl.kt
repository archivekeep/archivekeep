package org.archivekeep.app.core.operations.add

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.files.operations.indexupdate.IndexUpdateStructuredProgressTracker
import org.archivekeep.files.operations.indexupdate.IndexUpdateTextualProgressTracker
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.LoadableWithProgress
import org.archivekeep.utils.loading.flatMapLoadableFlow
import java.io.PrintWriter

class AddOperationSupervisorServiceImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
) : AddOperationSupervisorService {
    private val addPushOperations = singleInstanceWeakValueMap(::AddOperationImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, JobImpl>()

    override fun getAddOperation(repositoryURI: RepositoryURI): AddOperationSupervisor = addPushOperations[repositoryURI]

    inner class AddOperationImpl(
        val repositoryURI: RepositoryURI,
    ) : AddOperationSupervisor {
        override val currentJobFlow: StateFlow<JobImpl?> = jobGuards.stateHoldersWeakReference[repositoryURI].asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun prepare(): Flow<Loadable<AddOperationSupervisor.Preparation>> =
            repositoryService
                .getRepository(repositoryURI)
                .accessorFlow
                .flatMapLoadableFlow { repositoryAccess ->
                    repositoryAccess.observable.indexFlow
                        .conflate()
                        .flatMapLoadableFlow {
                            AddOperation(
                                subsetGlobs = listOf("."),
                                disableFilenameCheck = false,
                                disableMovesCheck = false,
                            ).prepare(repositoryAccess)
                                .map {
                                    when (it) {
                                        is LoadableWithProgress.Failed -> Loadable.Failed(it.throwable)
                                        is LoadableWithProgress.Loaded -> {
                                            Loadable.Loaded(
                                                AddOperationSupervisor.Preparation(
                                                    it.value,
                                                    launch = { launchOptions ->
                                                        val job =
                                                            JobImpl(
                                                                repositoryAccess,
                                                                it.value,
                                                                launchOptions,
                                                            )

                                                        jobGuards.launch(scope, Dispatchers.IO, repositoryURI, job)
                                                    },
                                                ),
                                            )
                                        }
                                        LoadableWithProgress.Loading -> Loadable.Loading
                                        is LoadableWithProgress.LoadingProgress -> {
                                            Loadable.Loaded(
                                                AddOperationSupervisor.Preparation(
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
        override val preparationResult: AddOperation.PreparationResult,
        override val launchOptions: AddOperation.LaunchOptions,
    ) : AddOperationSupervisor.Job,
        UniqueJobGuard.RunnableJob {
        private val executionLog = SyncFlowStringWriter()
        private val writter = IndexUpdateTextualProgressTracker(PrintWriter(executionLog.writer, true))

        private val indexUpdateProgressTracker = IndexUpdateStructuredProgressTracker()

        private val movesToExecute = (launchOptions.movesSubsetLimit?.intersect(preparationResult.moves.toSet()) ?: preparationResult.moves).toSet()
        private val filesToAdd = (launchOptions.addFilesSubsetLimit?.intersect(preparationResult.newFiles.toSet()) ?: preparationResult.newFiles).toSet()

        override val executionStateFlow: Flow<AddOperationSupervisor.ExecutionState.Running> =
            combine(
                indexUpdateProgressTracker.addProgressFlow,
                indexUpdateProgressTracker.moveProgressFlow,
                executionLog.string,
            ) { addProgress, moveProgress, log ->
                AddOperationSupervisor.ExecutionState.Running(
                    movesToExecute,
                    filesToAdd,
                    addProgress,
                    moveProgress,
                    log,
                )
            }

        private var job: Job? = null

        override suspend fun run(job: Job) {
            try {
                preparationResult.execute(
                    repositoryAccess,
                    launchOptions.movesSubsetLimit,
                    launchOptions.addFilesSubsetLimit,
                    writter,
                    indexUpdateProgressTracker,
                )
            } finally {
                executionLog.writer.flush()
                this.job = null
            }
        }

        override fun cancel() {
            job!!.cancel(message = "Cancelled by user")
            println("Cancelled")
        }
    }
}
