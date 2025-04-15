package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.waitLoadedValue

class StoragePartiallyResolved(
    scope: CoroutineScope,
    val uri: StorageURI,
    val knownStorage: KnownStorage,
    val storage: Storage,
) {
    val repositories: SharedFlow<List<StorageRepository>> = storage.repositories.waitLoadedValue().shareResourceIn(scope)

    val isLocal: Boolean
        get() = knownStorage.isLocal

    val label: String
        get() = knownStorage.label

    val namedReference =
        StorageNamedReference(
            uri,
            label,
        )

    val state = storage.state
}

data class StorageNamedReference(
    val uri: StorageURI,
    val displayName: String,
)
