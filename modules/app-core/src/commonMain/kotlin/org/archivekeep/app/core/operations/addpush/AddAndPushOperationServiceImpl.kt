package org.archivekeep.app.core.operations.addpush

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
import org.archivekeep.app.core.utils.UniqueJobGuard
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.operations.AbstractOperationJob
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.files.operations.indexupdate.IndexUpdateStructuredProgressTracker
import org.archivekeep.files.operations.sync.copyFile
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.LoadableWithProgress
import org.archivekeep.utils.loading.firstLoadedOrFailure

class AddAndPushOperationServiceImpl(
    private val scope: CoroutineScope,
    private val repositoryService: RepositoryService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AddAndPushOperationService {
    private val addPushOperations = singleInstanceWeakValueMap(::AddAndPushOperationImpl)

    val jobGuards = UniqueJobGuard<RepositoryURI, RepoAddPushJobImpl>()

    inner class RepoAddPushJobImpl(
        private val addPush: AddAndPushOperationImpl,
        override val addPreparationResult: AddOperation.PreparationResult,
        private val launchOptions: AddAndPushOperation.LaunchOptions,
    ) : AbstractOperationJob(),
        AddAndPushOperation.Job {
        private val structuredProgressTracker = IndexUpdateStructuredProgressTracker()

        val repositoryPushStatus =
            MutableStateFlow(
                launchOptions.selectedDestinationRepositories
                    .associateWith {
                        AddAndPushOperation.PushProgress(emptySet(), emptySet(), emptyMap(), false)
                    },
            )

        override val state =
            combine(
                structuredProgressTracker.addProgressFlow,
                structuredProgressTracker.moveProgressFlow,
                repositoryPushStatus,
                executionState,
            ) { addProgress, moveProgress, repositoryPushStatus, executionState ->
                AddAndPushOperation.JobState(
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

    override fun getAddPushOperation(repositoryURI: RepositoryURI) = this.addPushOperations[repositoryURI]

    inner class AddAndPushOperationImpl(
        val repositoryURI: RepositoryURI,
    ) : AddAndPushOperation {
        val repository = repositoryService.getRepository(repositoryURI)

        override val currentJobFlow = jobGuards.stateHoldersWeakReference[repositoryURI]

        override fun prepare(): Flow<AddAndPushOperation.State> =
            combineTransform(
                repository.accessorFlow,
                repository.localRepoStatus,
                repository.indexFlow,
            ) { accessor, _, _ ->
                // TODO: reuse already computed index and local repo status

                if (accessor is Loadable.Loaded) {
                    val preparationFlow =
                        AddOperation(
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
                                            AddAndPushOperation.ReadyAddPushProcess(
                                                preparedAddOperation,
                                                launch = { launchOptions ->
                                                    launch(preparedAddOperation, launchOptions)
                                                },
                                            ),
                                        )
                                    }
                                    LoadableWithProgress.Loading -> {}
                                    is LoadableWithProgress.LoadingProgress -> {
                                        emit(AddAndPushOperation.PreparingAddPushProcess(it.progress))
                                    }
                                }
                            }

                    emitAll(preparationFlow)
                }
            }.flowOn(ioDispatcher)

        internal fun launch(
            addPreparationResult: AddOperation.PreparationResult,
            launchOptions: AddAndPushOperation.LaunchOptions,
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
