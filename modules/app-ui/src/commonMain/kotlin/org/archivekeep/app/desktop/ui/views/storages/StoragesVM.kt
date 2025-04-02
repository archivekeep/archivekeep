package org.archivekeep.app.desktop.ui.views.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.combineToList
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.flatMapLatestLoadedData

class StoragesVM(
    val scope: CoroutineScope,
    val storageService: StorageService,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val allStorages =
        storageService
            .allStorages
            .flatMapLatestLoadedData { storages ->
                combineToList(
                    storages.map { storage ->
                        storage.repositories.map { repositories ->
                            Storage(
                                uri = storage.uri,
                                displayName = storage.knownStorage.label,
                                isLocal = storage.knownStorage.isLocal,
                                repositoriesInThisStorage = repositories.map { it.repositoryState.namedReference },
                            )
                        }
                    },
                )
            }.shareResourceIn(scope)

    class Storage(
        val uri: StorageURI,
        val displayName: String,
        val isLocal: Boolean,
        val repositoriesInThisStorage: List<NamedRepositoryReference>,
    )
}
