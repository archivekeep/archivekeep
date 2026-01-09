package org.archivekeep.files.procedures.addpush

import org.archivekeep.files.procedures.sync.job.SyncProcedureJobTask
import org.archivekeep.files.procedures.sync.log.SyncLogger
import org.archivekeep.files.procedures.sync.operations.CopyNewFileOperation
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.procedures.ProcedureExecutionContext
import org.archivekeep.utils.procedures.tasks.SequentialProcedureJobTaskGroup

class PushTask<ID>(
    private val repositoryProvider: suspend (ID) -> Repo,
    movesStep: SyncProcedureJobTask<RelocationApplyOperation>?,
    copyTask: SyncProcedureJobTask<CopyNewFileOperation>?,
    val repositoryURI: ID,
    val destinationRepoID: ID,
) : SequentialProcedureJobTaskGroup<ProcedureExecutionContext, SyncProcedureJobTask.Context>(
        subTasks =
            listOfNotNull(
                movesStep,
                copyTask,
            ),
        produceInnerContext =
            { context ->
                SyncProcedureJobTask.Context(
                    context,
                    repositoryProvider(repositoryURI),
                    repositoryProvider(destinationRepoID),
                    // TODO: get rid of this object
                    object : SyncLogger {
                        override fun onFileStored(filename: String) {
                        }

                        override fun onFileMoved(
                            from: String,
                            to: String,
                        ) {
                        }

                        override fun onFileDeleted(filename: String) {
                        }
                    },
                    prompter = { true },
                )
            },
    )
