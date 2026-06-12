package org.archivekeep.app.core.procedures.deletedcleanup

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
import org.archivekeep.files.procedures.deletedcleanup.DeletedFilesCleanupProcedure
import org.archivekeep.files.procedures.deletedcleanup.DeletedFilesCleanupStructuredProgressTracker
import org.archivekeep.files.procedures.deletedcleanup.DeletedFilesCleanupTextualProgressTracker
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import java.io.PrintWriter

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(CoreApplicationServiceScope::class)
class DeletedFilesCleanupProcedureSupervisorServiceImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
) : DeletedFilesCleanupProcedureSupervisorService {
    private val operations = singleInstanceWeakValueMap(::DeletedFilesCleanupProcedureSupervisorImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, JobImpl>()

    override fun getDeletedFilesCleanupOperation(repositoryURI: RepositoryURI): DeletedFilesCleanupProcedureSupervisor = operations[repositoryURI]

    inner class DeletedFilesCleanupProcedureSupervisorImpl(
        val repositoryURI: RepositoryURI,
    ) : DeletedFilesCleanupProcedureSupervisor {
        override val currentJobFlow: StateFlow<JobImpl?> = jobGuards.stateHoldersWeakReference[repositoryURI].asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun prepare(): Flow<Loadable<DeletedFilesCleanupProcedureSupervisor.Preparation>> =
            repositoryService
                .getRepository(repositoryURI)
                .accessorFlow
                .flatMapLoadableFlow { repositoryAccess ->
                    repositoryAccess.indexFlow
                        .flatMapLoadableFlow {
                            DeletedFilesCleanupProcedure()
                                .prepare(repositoryAccess)
                                .map {
                                    when (it) {
                                        is Loadable.Failed -> {
                                            Loadable.Failed(it.throwable)
                                        }

                                        is Loadable.Loaded -> {
                                            Loadable.Loaded(
                                                DeletedFilesCleanupProcedureSupervisor.Preparation(
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
        val preparationResult: DeletedFilesCleanupProcedure.PreparationResult,
        val launchOptions: DeletedFilesCleanupProcedure.LaunchOptions,
    ) : AbstractJobGuardRunnable(),
        JobWrapper<DeletedFilesCleanupProcedureSupervisor.JobState> {
        private val executionLog = SyncFlowStringWriter()
        private val writer = DeletedFilesCleanupTextualProgressTracker(PrintWriter(executionLog.writer, true))

        // TODO - this should be encapsulated into procedure's logic, not supervisor
        private val filesToRemove =
            (
                launchOptions.removeFilesSubsetLimit?.intersect(preparationResult.missingFiles.toSet())
                    ?: preparationResult.missingFiles
            ).toSet()

        private val structuredProgressTracker = DeletedFilesCleanupStructuredProgressTracker(filesToRemove)

        val job =
            object : AbstractProcedureJob() {
                override suspend fun execute(context: ProcedureExecutionContext) {
                    preparationResult.execute(
                        repositoryAccess,
                        launchOptions.removeFilesSubsetLimit,
                        writer,
                        structuredProgressTracker,
                    )
                }
            }

        override val state: Flow<DeletedFilesCleanupProcedureSupervisor.JobState> =
            combine(
                structuredProgressTracker.fileRemoveProgressFlow,
                executionLog.string,
                job.executionState,
            ) { fileRemoveProgress, log, jobState ->
                DeletedFilesCleanupProcedureSupervisor.JobState(
                    fileRemoveProgress,
                    log,
                    jobState,
                )
            }

        override suspend fun execute() {
            job.run()
        }
    }
}
