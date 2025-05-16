package org.archivekeep.app.core.procedures.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.ProcedureExecutionState
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.progress.OperationProgress
import org.archivekeep.files.procedures.sync.PreparedSyncProcedure
import org.archivekeep.files.procedures.sync.RelocationSyncMode
import org.archivekeep.files.procedures.sync.SyncOperation
import org.archivekeep.files.procedures.sync.SyncOperationGroup
import org.archivekeep.utils.loading.Loadable

interface RepoToRepoSync {
    val currentJobFlow: StateFlow<Job?>

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

    interface Job {
        val currentState: Flow<JobState>

        fun cancel()
    }

    sealed interface State {
        val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>

        data class Prepared(
            override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val preparedSyncProcedure: PreparedSyncProcedure,
            val startExecution: (limitToSubset: Set<SyncOperation>) -> Job,
        ) : State
    }

    data class JobState(
        override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
        val preparedSyncProcedure: PreparedSyncProcedure,
        val progress: StateFlow<List<SyncOperationGroup.Progress>>,
        val inProgressOperationsStats: StateFlow<List<OperationProgress>>,
        val progressLog: StateFlow<String>,
        val executionState: ProcedureExecutionState,
    ) : State
}
