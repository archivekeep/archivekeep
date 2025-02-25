package org.archivekeep.app.desktop.ui.views.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.utils.generics.sharedWhileSubscribed
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.combineToList
import org.archivekeep.utils.loading.flatMapLatestLoadedData

class StoragesVM(
    val scope: CoroutineScope,
    val storageService: StorageService,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val allStorages =
        storageService
            .allStorages
            .flatMapLatestLoadedData { storage ->
                combineToList(
                    storage.map { physicalMedium ->
                        physicalMedium.repositories.map { repositories ->
                            Storage(
                                uri = physicalMedium.uri,
                                displayName = physicalMedium.knownStorage.label,
                                repositoriesInThisStorage = repositories.map { it.repositoryState.namedReference },
                            )
                        }
                    },
                )
            }.sharedWhileSubscribed(scope)

    class Storage(
        val uri: StorageURI,
        val displayName: String,
        val repositoriesInThisStorage: List<NamedRepositoryReference>,
    )
}
