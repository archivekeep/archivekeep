package org.archivekeep.app.ui.dialogs.repository.procedures.sync.parts

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.State
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.RepoToRepoSyncUserFlow

@Composable
fun (ColumnScope).RepoToRepoSyncMainContents(userFlowState: RepoToRepoSyncUserFlow.State) {
    LoadableGuard(userFlowState.operation) { operation ->
        when (operation) {
            is State.Prepared -> SyncPreparedState(operation, userFlowState)
            is JobState -> SyncJobState(operation)
        }
    }
}

