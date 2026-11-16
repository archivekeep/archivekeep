package org.archivekeep.app.core.procedures.verify

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
import org.archivekeep.app.core.utils.generics.SyncFlowTextLinesWriter
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.procedures.verify.VerifyContentsProcedure
import org.archivekeep.files.procedures.verify.VerifyStructuredProgressTracker
import org.archivekeep.files.procedures.verify.VerifyTextualProgressTracker
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import java.io.PrintWriter

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(CoreApplicationServiceScope::class)
class VerifySupervisorServiceImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
) : VerifySupervisorService {
    private val operations = singleInstanceWeakValueMap(::VerifySupervisorImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, JobImpl>()

    override fun getVerifyOperation(repositoryURI: RepositoryURI): VerifySupervisor = operations[repositoryURI]

    inner class VerifySupervisorImpl(
        val repositoryURI: RepositoryURI,
    ) : VerifySupervisor {
        override val currentJobFlow: StateFlow<JobImpl?> = jobGuards.stateHoldersWeakReference[repositoryURI].asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun prepare(): Flow<Loadable<VerifySupervisor.Preparation>> =
            repositoryService
                .getRepository(repositoryURI)
                .accessorFlow
                .flatMapLoadableFlow { repositoryAccess ->
                    repositoryAccess.indexFlow
                        .flatMapLoadableFlow {
                            VerifyContentsProcedure()
                                .prepare(repositoryAccess)
                                .map {
                                    when (it) {
                                        is Loadable.Failed -> {
                                            Loadable.Failed(it.throwable)
                                        }

                                        is Loadable.Loaded -> {
                                            Loadable.Loaded(
                                                VerifySupervisor.Preparation(
                                                    it.value,
                                                    launch = {
                                                        val job =
                                                            JobImpl(
                                                                repositoryAccess,
                                                                it.value,
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
        val preparationResult: VerifyContentsProcedure.PreparationResult,
    ) : AbstractJobGuardRunnable(),
        JobWrapper<VerifySupervisor.JobState> {
        private val executionLog = SyncFlowTextLinesWriter()
        private val writer = VerifyTextualProgressTracker(PrintWriter(executionLog.writer, true))

        private val filesToVerify = preparationResult.filesToVerify.toSet()

        private val verifyProgressTracker = VerifyStructuredProgressTracker(filesToVerify)

        val job =
            object : AbstractProcedureJob() {
                override suspend fun execute(context: ProcedureExecutionContext) {
                    preparationResult.execute(
                        repositoryAccess,
                        writer.out,
                        writer,
                        verifyProgressTracker,
                    )
                }
            }

        override val state: Flow<VerifySupervisor.JobState> =
            combine(
                verifyProgressTracker.verifyProgressFlow,
                job.executionState,
            ) { verifyProgress, jobState ->
                VerifySupervisor.JobState(
                    verifyProgress,
                    executionLog.lines,
                    jobState,
                )
            }

        override suspend fun execute() {
            job.run()
        }
    }
}
