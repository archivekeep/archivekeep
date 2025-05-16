package org.archivekeep.app.core.procedures.addpush

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateStructuredProgressTracker
import org.archivekeep.files.procedures.sync.copyFile
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.utils.loading.firstLoadedOrFailure
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext

class AddAndPushProcedureJob(
    private val repositoryService: RepositoryService,
    val repositoryURI: RepositoryURI,
    val addPreparationResult: IndexUpdateProcedure.PreparationResult,
    private val launchOptions: AddAndPushProcedure.LaunchOptions,
) : AbstractProcedureJob() {
    private val structuredProgressTracker = IndexUpdateStructuredProgressTracker()

    val repositoryPushStatus =
        MutableStateFlow(
            launchOptions.selectedDestinationRepositories
                .associateWith {
                    AddAndPushProcedure.PushProgress(emptySet(), emptySet(), emptyMap(), false)
                },
        )

    val state =
        combine(
            structuredProgressTracker.addProgressFlow,
            structuredProgressTracker.moveProgressFlow,
            repositoryPushStatus,
            executionState,
            inProgressOperationsProgressFlow,
        ) { addProgress, moveProgress, repositoryPushStatus, executionState, inProgressOperationsProgress ->
            AddAndPushProcedure.JobState(
                launchOptions,
                addProgress,
                moveProgress,
                repositoryPushStatus,
                executionState,
                inProgressOperationsProgress,
            )
        }

    override suspend fun execute(context: ProcedureExecutionContext) {
        val repo =
            repositoryService
                .getRepository(repositoryURI)
                .accessorFlow
                .firstLoadedOrFailure()

        if (repo !is LocalRepo) {
            throw RuntimeException("not local repo: $repositoryURI")
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
                            context.runOperation { operationContext ->
                                copyFile(
                                    dst = destinationRepo,
                                    base = repo,
                                    filename = fileToPush,
                                    progressReport = operationContext::progressReport,
                                )
                            }

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
