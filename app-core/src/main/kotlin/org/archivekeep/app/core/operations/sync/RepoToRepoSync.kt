package org.archivekeep.app.core.operations.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.PreparedSyncOperation
import org.archivekeep.files.operations.RelocationSyncMode
import org.archivekeep.files.operations.SyncPlanStep
import org.archivekeep.utils.Loadable

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
        val currentState: StateFlow<JobState?>

        fun cancel()
    }

    sealed interface State {
        val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>

        data class Prepared(
            override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val preparedSyncOperation: PreparedSyncOperation,
            val startExecution: () -> Job,
        ) : State
    }

    sealed interface JobState : State {
        data class Running(
            override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val preparedSyncOperation: PreparedSyncOperation,
            val progressLog: StateFlow<String>,
            val progress: StateFlow<List<SyncPlanStep.Progress>>,
            val job: Job,
        ) : JobState

        data class Finished(
            override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
            val preparedSyncOperation: PreparedSyncOperation,
            val progressLog: String,
            val success: Boolean,
            val cancelled: Boolean,
        ) : JobState
    }
}
