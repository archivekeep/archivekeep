package org.archivekeep.app.desktop.ui.utils

import org.archivekeep.app.core.domain.storages.StorageRepository

fun contextualStorageReference(
    baseRepositoryName: String,
    storageRepository: StorageRepository,
) = if (baseRepositoryName != storageRepository.displayName) {
    "${storageRepository.displayName} in ${storageRepository.storage.displayName}"
} else {
    storageRepository.storage.displayName
}

fun filesAutoPlural(items: Collection<*>): String =
    if (items.size == 1) {
        "file"
    } else {
        "files"
    }
