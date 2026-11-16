package org.archivekeep.app.ui.utils

import androidx.compose.runtime.Composable
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_n_files_plural
import org.archivekeep.ui.resources.common_n_files_singular
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.jetbrains.compose.resources.stringResource
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

@Composable
fun filesAutoPluralTranslated(count: Int, prefix: String = ""): String =
    if (count == 1) {
        stringResource(Res.string.common_n_files_singular, prefix)
    } else {
        stringResource(Res.string.common_n_files_plural, count, prefix)
    }

@Composable
fun filesAutoPluralTranslated(items: Collection<*>, prefix: String = ""): String =
    filesAutoPluralTranslated(items.size, prefix)
