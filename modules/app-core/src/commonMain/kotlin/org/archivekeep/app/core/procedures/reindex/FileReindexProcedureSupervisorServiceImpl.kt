package org.archivekeep.app.core.procedures.reindex

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.app.core.utils.AbstractJobGuardRunnable
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.SyncFlowStringWriter
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.procedures.reindex.FileReindexProcedure
import org.archivekeep.files.procedures.reindex.FileReindexStructuredProgressTracker
import org.archivekeep.files.procedures.reindex.FileReindexTextualProgressTracker
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import java.io.PrintWriter

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(CoreApplicationServiceScope::class)
class FileReindexProcedureSupervisorServiceImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
) : FileReindexProcedureSupervisorService {
    private val addPushOperations = singleInstanceWeakValueMap(::IndexUpdateProcedureImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, JobImpl>()

    override fun getFileReindexOperation(repositoryURI: RepositoryURI): FileReindexProcedureSupervisor = addPushOperations[repositoryURI]

    inner class IndexUpdateProcedureImpl(
        val repositoryURI: RepositoryURI,
    ) : FileReindexProcedureSupervisor {
        override val currentJobFlow: StateFlow<JobImpl?> = jobGuards.stateHoldersWeakReference[repositoryURI].asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun prepare(): Flow<Loadable<FileReindexProcedureSupervisor.Preparation>> =
            repositoryService
                .getRepository(repositoryURI)
                .accessorFlow
                .flatMapLoadableFlow { repositoryAccess ->
                    repositoryAccess.indexFlow
                        .flatMapLoadableFlow {
                            FileReindexProcedure()
                                .prepare(repositoryAccess)
                                .map {
                                    when (it) {
                                        is Loadable.Failed -> {
                                            Loadable.Failed(it.throwable)
                                        }

                                        is Loadable.Loaded -> {
                                            Loadable.Loaded(
                                                FileReindexProcedureSupervisor.Preparation(
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

                                        Loadable.Loading -> {
                                            Loadable.Loading
                                        }
                                    }
                                }
                        }.flowOn(Dispatchers.IO)
                }
    }

    inner class JobImpl(
        private val repositoryAccess: Repo,
        val preparationResult: FileReindexProcedure.PreparationResult,
        val launchOptions: FileReindexProcedure.LaunchOptions,
    ) : AbstractJobGuardRunnable(),
        JobWrapper<FileReindexProcedureSupervisor.JobState> {
        private val executionLog = SyncFlowStringWriter()
        private val writter = FileReindexTextualProgressTracker(PrintWriter(executionLog.writer, true))

        // TODO - this should be encapsulated into procedure's logic, not supervisor
        private val filesToReindex =
            (
                launchOptions.reindexFilesSubsetLimit?.intersect(preparationResult.modifiedIndexedFiles.toSet())
                    ?: preparationResult.modifiedIndexedFiles
            ).toSet()

        private val indexUpdateProgressTracker = FileReindexStructuredProgressTracker(filesToReindex)

        val job =
            object : AbstractProcedureJob() {
                override suspend fun execute(context: ProcedureExecutionContext) {
                    preparationResult.execute(
                        repositoryAccess,
                        launchOptions.reindexFilesSubsetLimit,
                        writter,
                        indexUpdateProgressTracker,
                    )
                }
            }

        override val state: Flow<FileReindexProcedureSupervisor.JobState> =
            combine(
                indexUpdateProgressTracker.fileReindexProgressFlow,
                executionLog.string,
                job.executionState,
            ) { reindexProgress, log, jobState ->
                FileReindexProcedureSupervisor.JobState(
                    reindexProgress,
                    log,
                    jobState,
                )
            }

        override suspend fun execute() {
            job.run()
        }
    }
}
