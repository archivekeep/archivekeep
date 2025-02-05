package org.archivekeep.app.core.operations.derived

import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.core.operations.CompareOperation
import org.archivekeep.core.operations.PreparedSyncOperation
import org.archivekeep.core.operations.SyncPlanStep

sealed interface PreparedOrRunningSync : SyncOperationExecution

sealed interface PreparedRunningOrCompletedSync : SyncOperationExecution

sealed interface RunningOrCompletedSync : SyncOperationExecution

sealed interface SyncOperationExecution {
    val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>

    data class NotRunning(
        override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
    ) : SyncOperationExecution

    data class Prepared(
        override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
        val preparedSyncOperation: PreparedSyncOperation,
        val startExecution: () -> DefaultSyncService.Operation,
    ) : SyncOperationExecution,
        PreparedOrRunningSync,
        PreparedRunningOrCompletedSync

    data class Running(
        override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
        val preparedSyncOperation: PreparedSyncOperation,
        val progressLog: StateFlow<String>,
        val progress: StateFlow<List<SyncPlanStep.Progress>>,
        val operation: DefaultSyncService.Operation,
    ) : SyncOperationExecution,
        PreparedOrRunningSync,
        PreparedRunningOrCompletedSync,
        RunningOrCompletedSync

    data class Finished(
        override val comparisonResult: OptionalLoadable.LoadedAvailable<CompareOperation.Result>,
        val preparedSyncOperation: PreparedSyncOperation,
        val progressLog: String,
        val success: Boolean,
        val cancelled: Boolean,
    ) : SyncOperationExecution,
        PreparedRunningOrCompletedSync,
        RunningOrCompletedSync
}
