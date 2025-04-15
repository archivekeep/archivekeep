package org.archivekeep.app.desktop.ui.views.storages

import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.app.core.utils.identifiers.StorageURI

class StoragesViewState(
    resolvedStorages: List<Storage>,
) {
    val localStorages = resolvedStorages.filter { it.isLocal }

    val externalStorages = resolvedStorages.filter { !it.isLocal && !it.isOnline }
    val onlineStorages = resolvedStorages.filter { !it.isLocal && it.isOnline }

    class Storage(
        val uri: StorageURI,
        val displayName: String,
        val isLocal: Boolean,
        val isOnline: Boolean,
        val repositoriesInThisStorage: List<NamedRepositoryReference>,
    )
}
