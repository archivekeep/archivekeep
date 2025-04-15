package org.archivekeep.app.desktop.ui.views.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.utils.combineToObject
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.flatMapLatestLoadedData

class StoragesViewModel(
    val scope: CoroutineScope,
    val storageService: StorageService,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val state =
        storageService
            .allStorages
            .flatMapLatestLoadedData { storages ->
                combineToObject(
                    storages.map(::resolveStorage),
                ) { resolvedStorages ->
                    StoragesViewState(resolvedStorages.toList())
                }
            }.shareResourceIn(scope)

    private fun resolveStorage(storage: org.archivekeep.app.core.domain.storages.Storage) =
        storage.repositories.map { repositories ->
            StoragesViewState.Storage(
                uri = storage.uri,
                displayName = storage.knownStorage.label,
                isLocal = storage.knownStorage.isLocal,
                isOnline = storage.connection.information is StorageInformation.OnlineStorage,
                repositoriesInThisStorage = repositories.map { it.repositoryState.namedReference },
            )
        }
}
