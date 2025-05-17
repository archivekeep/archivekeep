package org.archivekeep.app.core.procedures.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.PreparedSyncProcedure
import org.archivekeep.files.procedures.sync.RelocationSyncMode
import org.archivekeep.files.procedures.sync.SyncOperation
import org.archivekeep.files.procedures.sync.SyncOperationGroup
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.procedures.OperationProgress
import org.archivekeep.utils.procedures.ProcedureExecutionState

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
            val preparedSyncProcedure: PreparedSyncProcedure,
            val startExecution: (limitToSubset: Set<SyncOperation>) -> JobWrapper<JobState>,
        ) : State
    }

    data class JobState(
        val progress: StateFlow<List<SyncOperationGroup.Progress>>,
        val inProgressOperationsProgress: StateFlow<List<OperationProgress>>,
        val progressLog: StateFlow<String>,
        val executionState: ProcedureExecutionState,
    ) : State
}
