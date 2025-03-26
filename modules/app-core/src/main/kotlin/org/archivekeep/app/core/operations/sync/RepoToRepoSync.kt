package org.archivekeep.app.core.operations.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.sync.PreparedSyncOperation
import org.archivekeep.files.operations.sync.RelocationSyncMode
import org.archivekeep.files.operations.sync.SyncSubOperation
import org.archivekeep.files.operations.sync.SyncSubOperationGroup
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
        val currentState: StateFlow<JobState>

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

    sealed interface JobState : State {
        data class Created(
            override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val preparedSyncOperation: PreparedSyncOperation,
            val job: Job,
        ) : JobState

        data class Running(
            override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val preparedSyncOperation: PreparedSyncOperation,
            val progressLog: StateFlow<String>,
            val progress: StateFlow<List<SyncSubOperationGroup.Progress>>,
            val job: Job,
        ) : JobState

        data class Finished(
            override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val preparedSyncOperation: PreparedSyncOperation,
            val progress: List<SyncSubOperationGroup.Progress>,
            val progressLog: String,
            val error: Throwable?,
        ) : JobState {
            val success = error == null

            val cancelled = error != null && error is CancellationException
        }
    }
}
