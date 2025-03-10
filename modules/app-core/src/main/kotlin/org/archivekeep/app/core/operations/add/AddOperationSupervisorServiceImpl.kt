package org.archivekeep.app.core.operations.add

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.operations.add.AddOperationSupervisor.AddProgress
import org.archivekeep.app.core.operations.add.AddOperationSupervisor.MoveProgress
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.files.operations.AddOperationTextWriter
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.loading.mapLoadedData
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
        override fun prepare(): Flow<Loadable<AddOperationSupervisor.Prepared>> =
            repositoryService
                .getRepository(repositoryURI)
                .accessorFlow
                .flatMapLoadableFlow { repositoryAccess ->
                    repositoryAccess.observable.indexFlow
                        .conflate()
                        .mapLoadedData {
                            val preparedResult =
                                AddOperation(
                                    subsetGlobs = listOf("."),
                                    disableFilenameCheck = false,
                                    disableMovesCheck = false,
                                ).prepare(repositoryAccess)

                            AddOperationSupervisor.Prepared(
                                preparedResult,
                                launch = { launchOptions ->
                                    val job =
                                        JobImpl(
                                            repositoryAccess,
                                            preparedResult,
                                            launchOptions,
                                        )

                                    jobGuards.launch(scope, Dispatchers.IO, repositoryURI, job)
                                },
                            )
                        }.flowOn(Dispatchers.IO)
                }
    }

    inner class JobImpl(
        private val repositoryAccess: Repo,
        override val preparationResult: AddOperation.PreparationResult,
        override val launchOptions: AddOperation.LaunchOptions,
    ) : AddOperationSupervisor.Job,
        UniqueJobGuard.RunnableJob {
        private val executeResult = SyncFlowStringWriter()
        private val writter = AddOperationTextWriter(PrintWriter(executeResult.writer, true))

        private val addProgressFlow = MutableStateFlow(AddProgress(emptySet(), emptyMap(), false))
        private val moveProgressFlow = MutableStateFlow(MoveProgress(emptySet(), emptyMap(), false))

        override val executionStateFlow: Flow<AddOperationSupervisor.ExecutionState.Running> =
            combine(
                addProgressFlow,
                moveProgressFlow,
                executeResult.string,
            ) { addProgress, moveProgress, log ->
                AddOperationSupervisor.ExecutionState.Running(
                    addProgress,
                    moveProgress,
                    log,
                )
            }

        private var job: Job? = null

        override suspend fun run(job: Job) {
            try {
                preparationResult.executeMovesReindex(
                    repositoryAccess,
                    launchOptions.movesSubsetLimit,
                ) { move ->
                    writter.onMoveCompleted(move)
                    moveProgressFlow.update {
                        it.copy(
                            moved = it.moved + setOf(move),
                        )
                    }
                }
                moveProgressFlow.update {
                    it.copy(finished = true)
                }

                preparationResult.executeAddNewFiles(
                    repositoryAccess,
                    launchOptions.addFilesSubsetLimit,
                ) { add ->
                    writter.onAddCompleted(add)
                    addProgressFlow.update {
                        it.copy(
                            added = it.added + setOf(add),
                        )
                    }
                }
                addProgressFlow.update {
                    it.copy(finished = true)
                }
            } finally {
                executeResult.writer.flush()
                this.job = null
            }
        }

        override fun cancel() {
            job!!.cancel(message = "Cancelled by user")
            println("Cancelled")
        }
    }
}
