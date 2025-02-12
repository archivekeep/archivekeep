package org.archivekeep.app.core.operations.derived

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.operations.derived.DefaultSyncService.Operation
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.RelocationSyncMode
import org.archivekeep.utils.Loadable

interface RepoToRepoSync {
    val fromURI: RepositoryURI
    val otherURI: RepositoryURI
    val currentlyRunningOperationFlow: StateFlow<Operation?>
    val stateFlow: Flow<OptionalLoadable<State>>

    fun prepare(relocationSyncMode: RelocationSyncMode): Flow<Loadable<SyncOperationExecution.Prepared>>

    data class State(
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
}
