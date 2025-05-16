package org.archivekeep.app.core.procedures.addpush

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.AbstractJobGuardRunnable
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.LoadableWithProgress

class AddAndPushProcedureServiceImpl(
    private val scope: CoroutineScope,
    private val repositoryService: RepositoryService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AddAndPushProcedureService {
    private val addPushOperations = singleInstanceWeakValueMap(::AddAndPushProcedureImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, JobWrapperImpl>()

    class JobWrapperImpl(
        repositoryService: RepositoryService,
        repositoryURI: RepositoryURI,
        override val addPreparationResult: IndexUpdateProcedure.PreparationResult,
        launchOptions: AddAndPushProcedure.LaunchOptions,
    ) : AbstractJobGuardRunnable(),
        AddAndPushProcedure.JobWrapper {
        private val procedureJob =
            AddAndPushProcedureJob(
                repositoryService,
                repositoryURI,
                addPreparationResult,
                launchOptions,
            )

        override val state = procedureJob.state

        override suspend fun execute() {
            procedureJob.run()
        }
    }

    override fun getAddAndPushProcedure(repositoryURI: RepositoryURI) = this.addPushOperations[repositoryURI]

    inner class AddAndPushProcedureImpl(
        val repositoryURI: RepositoryURI,
    ) : AddAndPushProcedure {
        val repository = repositoryService.getRepository(repositoryURI)

        override val currentJobFlow = jobGuards.stateHoldersWeakReference[repositoryURI]

        override fun prepare(): Flow<AddAndPushProcedure.State> =
            combineTransform(
                repository.accessorFlow,
                repository.localRepoStatus,
                repository.indexFlow,
            ) { accessor, _, _ ->
                // TODO: reuse already computed index and local repo status

                if (accessor is Loadable.Loaded) {
                    val preparationFlow =
                        IndexUpdateProcedure(
                            subsetGlobs = listOf("."),
                            disableFilenameCheck = false,
                            disableMovesCheck = false,
                        ).prepare(accessor.value)
                            .transform {
                                when (it) {
                                    is LoadableWithProgress.Failed -> throw it.throwable
                                    is LoadableWithProgress.Loaded -> {
                                        val preparedAddOperation = it.value

                                        emit(
                                            AddAndPushProcedure.ReadyAddPushProcess(
                                                preparedAddOperation,
                                                launch = { launchOptions ->
                                                    launch(preparedAddOperation, launchOptions)
                                                },
                                            ),
                                        )
                                    }
                                    LoadableWithProgress.Loading -> {}
                                    is LoadableWithProgress.LoadingProgress -> {
                                        emit(AddAndPushProcedure.PreparingAddPushProcess(it.progress))
                                    }
                                }
                            }

                    emitAll(preparationFlow)
                }
            }.flowOn(ioDispatcher)

        internal fun launch(
            addPreparationResult: IndexUpdateProcedure.PreparationResult,
            launchOptions: AddAndPushProcedure.LaunchOptions,
        ) {
            val newOperation =
                JobWrapperImpl(
                    repositoryService,
                    repositoryURI,
                    addPreparationResult,
                    launchOptions,
                )

            jobGuards.launch(
                scope,
                ioDispatcher,
                repositoryURI,
                newOperation,
            )
        }
    }
}
