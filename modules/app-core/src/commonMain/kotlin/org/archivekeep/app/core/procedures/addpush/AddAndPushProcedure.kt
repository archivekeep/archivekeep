package org.archivekeep.app.core.procedures.addpush

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.procedures.addpush.AddAndPushProcedureJob
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.DiscoveryResult.Move
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.DiscoveryResult.NewFile

interface AddAndPushProcedure {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    fun prepare(): Flow<State>

    sealed interface State

    data object NotReadyAddPushProcess : State

    data class PreparingAddPushProcess(
        val addPreparationProgress: IndexUpdateProcedure.DiscoveryProgress,
    ) : State

    data class ReadyAddPushProcess(
        val addPreprationResult: IndexUpdateProcedure.DiscoveryResult,
        val launch: (options: LaunchOptions) -> Unit,
    ) : State

    data class LaunchOptions(
        val filesToAdd: Set<NewFile>,
        val movesToExecute: Set<Move>,
        val selectedDestinationRepositories: Set<RepositoryURI>,
    )

    data class JobState(
        val jobState: AddAndPushProcedureJob.State<RepositoryURI>,
    ) : State
}
