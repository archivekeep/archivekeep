package org.archivekeep.app.ui.utils

import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.mapIfLoadedOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun contextualStorageReference(
    baseRepositoryName: String,
    storageRepository: StorageRepository,
) = if (baseRepositoryName != storageRepository.displayName) {
    "${storageRepository.displayName} in ${storageRepository.storage.displayName}"
} else {
    storageRepository.storage.displayName
}

fun combineTexts(vararg texts: OptionalLoadable<List<String>>): OptionalLoadable<List<String>> {
    if (texts.any { it is OptionalLoadable.Loading }) {
        return OptionalLoadable.Loading
    }
    if (texts.any { it is OptionalLoadable.Failed }) {
        return texts.first { it is OptionalLoadable.Failed }
    }

    return OptionalLoadable.LoadedAvailable(texts.map { it.mapIfLoadedOrNull { it } ?: emptyList() }.flatten())
}

fun (Duration).toUiString(): String =
    if (this < 1.minutes) {
        "${this.inWholeSeconds}s"
    } else {
        "${this.inWholeMinutes}m ${this.inWholeSeconds - this.inWholeMinutes * 60}s"
    }
