package org.archivekeep.app.core.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.core.repo.remote.grpc.BasicAuthCredentials

interface AddRemoteRepositoryOperation {
    val addStatus: StateFlow<AddStatus?>
    val completed: StateFlow<Boolean>

    interface Factory {
        fun create(
            scope: CoroutineScope,
            url: String,
            credentials: BasicAuthCredentials?,
        ): AddRemoteRepositoryOperation
    }

    sealed interface AddStatus {
        data object Adding : AddStatus

        data object AddSuccessful : AddStatus

        data class AddFailed(
            val reason: String,
            val cause: Throwable?,
        ) : AddStatus

        data object RequiresCredentials : AddStatus

        data object WrongCredentials : AddStatus
    }

    fun cancel()
}
