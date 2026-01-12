package org.archivekeep.app.core.procedures.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.files.api.repository.operations.CompareOperation
import org.archivekeep.files.procedures.sync.discovery.DiscoveredSync
import org.archivekeep.files.procedures.sync.discovery.RelocationSyncMode
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.archivekeep.utils.procedures.operations.OperationProgress
import org.archivekeep.utils.procedures.tasks.TaskExecutionProgressSummary

interface RepoToRepoSync {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    val compareStateFlow: Flow<OptionalLoadable<CompareState>>

    fun prepare(relocationSyncMode: RelocationSyncMode): Flow<Loadable<State.Prepared>>

    data class CompareState(
        val compareOperationResult: CompareOperation.Result,
    ) {
        val baseTotal = compareOperationResult.allBaseFiles.size
        val missingBaseInOther = compareOperationResult.unmatchedBaseExtras.size
        val otherTotal = compareOperationResult.allOtherFiles.size
        val missingOtherInBase = compareOperationResult.unmatchedOtherExtras.size
        val relocations = compareOperationResult.relocations.size

        val fullySynced: Boolean
            get() = missingBaseInOther == 0 && missingOtherInBase == 0
    }

    sealed interface State {
        data class Prepared(
            val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val discoveredSync: DiscoveredSync,
            val startExecution: (limitToSubset: Set<SyncOperation>) -> JobWrapper<JobState>,
        ) : State
    }

    data class JobState(
        val progress: StateFlow<TaskExecutionProgressSummary.Group>,
        val inProgressOperationsProgress: StateFlow<List<OperationProgress>>,
        val progressLog: StateFlow<String>,
        val errorLog: StateFlow<String>,
        val executionState: ProcedureExecutionState,
    ) : State
}
