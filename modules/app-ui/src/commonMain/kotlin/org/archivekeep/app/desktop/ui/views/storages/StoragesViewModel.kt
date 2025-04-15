package org.archivekeep.app.desktop.ui.views.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.utils.combineToObject
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.flatMapLatestLoadedData
import org.archivekeep.utils.loading.waitLoadedValue

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

    private fun resolveStorage(storage: Storage) =
        combine(
            storage.repositories.waitLoadedValue(),
            storage.knownStorageFlow.waitLoadedValue(),
        ) { repositories, knownStorage ->
            StoragesViewState.Storage(
                uri = storage.uri,
                displayName = knownStorage.label,
                isLocal = knownStorage.isLocal,
                isOnline = storage.connection.information is StorageInformation.OnlineStorage,
                repositoriesInThisStorage = repositories.map { it.repositoryState.namedReference },
            )
        }
}
