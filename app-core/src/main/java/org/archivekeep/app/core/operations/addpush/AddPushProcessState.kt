package org.archivekeep.app.core.operations.addpush

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.operations.derived.IndexStatus
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface RepoAddPush {
    val currentJobFlow: StateFlow<Job?>
    val stateFlow: Flow<State>

    interface Job {
        val originalIndexStatus: IndexStatus
        val state: Flow<State>

        fun cancel()
    }

    sealed interface State

    data object NotReadyAddPushProcess : State

    data class ReadyAddPushProcess(
        val indexStatus: IndexStatus,
        val launch: (options: LaunchOptions) -> Unit,
    ) : State

    data class LaunchOptions(
        val selectedFiles: Set<String>,
        val selectedDestinationRepositories: Set<RepositoryURI>,
    )

    data class LaunchedAddPushProcess(
        val originalIndexStatus: IndexStatus,
        val options: LaunchOptions,
        val addProgress: AddProgress,
        val pushProgress: Map<RepositoryURI, PushProgress>,
        val finished: Boolean,
    ) : State

    data class AddProgress(
        val added: Set<String>,
        val error: Map<String, Any>,
        val finished: Boolean,
    )

    data class PushProgress(
        val added: Set<String>,
        val error: Map<String, Any>,
        val finished: Boolean,
    )
}
