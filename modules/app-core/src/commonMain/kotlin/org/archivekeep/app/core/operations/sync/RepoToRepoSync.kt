package org.archivekeep.app.core.operations.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.sync.PreparedSyncOperation
import org.archivekeep.files.operations.sync.RelocationSyncMode
import org.archivekeep.files.operations.sync.SyncSubOperation
import org.archivekeep.files.operations.sync.SyncSubOperationGroup
import org.archivekeep.files.operations.tasks.InProgressOperationStats
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
            val preparedSyncOperation: PreparedSyncOperation,
            val startExecution: (limitToSubset: Set<SyncSubOperation>) -> Job,
        ) : State
    }

    data class JobState(
        override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
        val preparedSyncOperation: PreparedSyncOperation,
        val progress: StateFlow<List<SyncSubOperationGroup.Progress>>,
        val inProgressOperationsStats: StateFlow<List<InProgressOperationStats>>,
        val progressLog: StateFlow<String>,
        val executionState: OperationExecutionState,
    ) : State
}
