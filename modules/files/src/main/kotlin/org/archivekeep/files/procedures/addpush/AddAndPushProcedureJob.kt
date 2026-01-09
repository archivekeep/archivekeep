package org.archivekeep.files.procedures.addpush

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.Move
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.NewFile
import org.archivekeep.files.procedures.indexupdate.IndexUpdateStructuredProgressTracker
import org.archivekeep.files.procedures.sync.discovery.DiscoveredNewFilesGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredRelocationsMoveApplyGroup
import org.archivekeep.files.procedures.sync.operations.CopyNewFileOperation
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.combineToList
import org.archivekeep.utils.procedures.AbstractProcedureJob
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.archivekeep.utils.procedures.operations.OperationProgress
import org.archivekeep.utils.procedures.tasks.TaskExecutionProgressSummary

class AddAndPushProcedureJob<ID>(
    private val repositoryProvider: suspend (ID) -> Repo,
    val repositoryURI: ID,
    val addPreparationResult: IndexUpdateProcedure.PreparationResult,
    val filesToAdd: Set<NewFile>,
    val movesToExecute: Set<Move>,
    val selectedDestinationRepositories: Set<ID>,
) : AbstractProcedureJob() {
    data class State<ID>(
        val addProgress: IndexUpdateAddProgress,
        val moveProgress: IndexUpdateMoveProgress,
        val pushProgress: List<Pair<PushTask<ID>, TaskExecutionProgressSummary.Group>>,
        val executionState: ProcedureExecutionState,
        val inProgressOperationsProgress: List<OperationProgress>,
    )

    val structuredProgressTracker =
        IndexUpdateStructuredProgressTracker(
            filesToAdd.map { it.fileName }.toSet(),
            movesToExecute,
        )

    val pushTasks =
        selectedDestinationRepositories
            .map { destinationRepoID ->
                val movesTask =
                    DiscoveredRelocationsMoveApplyGroup(
                        toApply = movesToExecute.map { it.toSyncRelocationOperation() }.toList(),
                        toIgnore = emptyList(),
                    ).let {
                        if (it.isNoOp()) null else it.createJobTask(null)
                    }

                val copyTask =
                    DiscoveredNewFilesGroup(
                        unmatchedBaseExtras =
                            filesToAdd
                                .map {
                                    CopyNewFileOperation(
                                        CompareOperation.Result.ExtraGroup(
                                            // TODO: this is ugly
                                            checksum = "UNKNOWN-NOT-REALLY-USED",
                                            fileSize = it.fileSize,
                                            filenames = listOf(it.fileName),
                                        ),
                                    )
                                }.toList(),
                    ).let {
                        if (it.isNoOp()) null else it.createJobTask(null)
                    }

                PushTask(
                    repositoryProvider,
                    movesTask,
                    copyTask,
                    repositoryURI,
                    destinationRepoID,
                )
            }

    val pushStatuses =
        combineToList(
            pushTasks.map { task ->
                task.executionProgressSummaryStateFlow.map { Pair(task, it) }
            },
        )

    val state =
        combine(
            structuredProgressTracker.addProgressFlow,
            structuredProgressTracker.moveProgressFlow,
            pushStatuses,
            executionState,
            inProgressOperationsProgressFlow,
        ) { addProgress, moveProgress, repositoryPushStatus, executionState, inProgressOperationsProgress ->
            State(
                addProgress,
                moveProgress,
                repositoryPushStatus,
                executionState,
                inProgressOperationsProgress,
            )
        }

    override suspend fun execute(context: ProcedureExecutionContext) {
        val repo = repositoryProvider(repositoryURI)

        if (repo !is LocalRepo) {
            throw RuntimeException("not local repo: $repositoryURI")
        }

        addPreparationResult.execute(
            repo,
            movesToExecute,
            filesToAdd.map { it.fileName }.toSet(),
            structuredProgressTracker,
        )

        coroutineScope {
            pushTasks
                .map {
                    launch { it.execute(context) }
                }.joinAll()
        }
    }
}
