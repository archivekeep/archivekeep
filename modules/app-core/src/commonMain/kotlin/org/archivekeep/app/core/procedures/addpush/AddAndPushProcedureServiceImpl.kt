package org.archivekeep.app.core.procedures.addpush

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.procedures.utils.AbstractProcedureJob
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateStructuredProgressTracker
import org.archivekeep.files.procedures.sync.copyFile
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.LoadableWithProgress
import org.archivekeep.utils.loading.firstLoadedOrFailure

class AddAndPushProcedureServiceImpl(
    private val scope: CoroutineScope,
    private val repositoryService: RepositoryService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AddAndPushProcedureService {
    private val addPushOperations = singleInstanceWeakValueMap(::AddAndPushProcedureImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, RepoAddPushJobImpl>()

    inner class RepoAddPushJobImpl(
        private val addPush: AddAndPushProcedureImpl,
        override val addPreparationResult: IndexUpdateProcedure.PreparationResult,
        private val launchOptions: AddAndPushProcedure.LaunchOptions,
    ) : AbstractProcedureJob(),
        AddAndPushProcedure.Job {
        private val structuredProgressTracker = IndexUpdateStructuredProgressTracker()

        val repositoryPushStatus =
            MutableStateFlow(
                launchOptions.selectedDestinationRepositories
                    .associateWith {
                        AddAndPushProcedure.PushProgress(emptySet(), emptySet(), emptyMap(), false)
                    },
            )

        override val state =
            combine(
                structuredProgressTracker.addProgressFlow,
                structuredProgressTracker.moveProgressFlow,
                repositoryPushStatus,
                executionState,
            ) { addProgress, moveProgress, repositoryPushStatus, executionState ->
                AddAndPushProcedure.JobState(
                    addPreparationResult,
                    launchOptions,
                    addProgress,
                    moveProgress,
                    repositoryPushStatus,
                    executionState,
                )
            }

        override suspend fun execute() {
            val repo =
                repositoryService
                    .getRepository(addPush.repositoryURI)
                    .accessorFlow
                    .firstLoadedOrFailure()

            if (repo !is LocalRepo) {
                throw RuntimeException("not local repo: ${addPush.repositoryURI}")
            }

            addPreparationResult.execute(
                repo,
                launchOptions.movesToExecute,
                launchOptions.filesToAdd,
                structuredProgressTracker,
            )

            coroutineScope {
                launchOptions
                    .selectedDestinationRepositories
                    .map { destinationRepoID ->
                        launch {
                            println("launched: $destinationRepoID")

                            val destinationRepo =
                                repositoryService
                                    .getRepository(destinationRepoID)
                                    .accessorFlow
                                    .firstLoadedOrFailure()

                            launchOptions.movesToExecute.forEach { move ->
                                destinationRepo.move(move.from, move.to)

                                repositoryPushStatus.update {
                                    it.mapValues { (k, v) ->
                                        if (k == destinationRepoID) {
                                            v.copy(
                                                moved = v.moved + setOf(move),
                                            )
                                        } else {
                                            v
                                        }
                                    }
                                }
                            }

                            launchOptions.filesToAdd.forEach { fileToPush ->
                                copyFile(
                                    dst = destinationRepo,
                                    base = repo,
                                    filename = fileToPush,
                                    progressReport = { /* TODO */ },
                                )

                                println("copied: $destinationRepoID - $fileToPush")

                                repositoryPushStatus.update {
                                    it.mapValues { (k, v) ->
                                        if (k == destinationRepoID) {
                                            v.copy(
                                                added = v.added + setOf(fileToPush),
                                            )
                                        } else {
                                            v
                                        }
                                    }
                                }
                            }
                        }
                    }.joinAll()
            }
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
                RepoAddPushJobImpl(
                    this,
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
