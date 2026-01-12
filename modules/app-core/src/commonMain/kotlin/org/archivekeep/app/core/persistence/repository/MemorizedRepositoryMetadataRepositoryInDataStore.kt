package org.archivekeep.app.core.persistence.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapToOptionalLoadable
import java.io.File

private val REMEMBERED_REPOSITORY_METADATA_KEY = stringSetPreferencesKey("remembered_repository_metadata")

class MemorizedRepositoryMetadataRepositoryInDataStore(
    val scope: CoroutineScope,
    val datastoreFile: File,
) : MemorizedRepositoryMetadataRepository {
    private val datastore =
        PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            produceFile = { datastoreFile },
        )

    val rememberedRepositoriesMetadata =
        datastore.data
            .map(::getRememberedRepositoriesMetadataFromPreferences)
            .onEach {
                println("Loaded repository metadata: $it")
            }.shareResourceIn(scope)

    override fun repositoryCachedMetadataFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepositoryMetadata>> =
        rememberedRepositoriesMetadata
            .mapToOptionalLoadable("Get memorized metadata for URI: $uri") { it[uri] }

    override suspend fun updateRepositoryMemorizedMetadataIfDiffers(
        uri: RepositoryURI,
        metadata: RepositoryMetadata?,
    ) {
        val rememberedMetadata = rememberedRepositoriesMetadata.first()[uri]

        if (metadata != rememberedMetadata) {
            println(
                "Remembered metadata for $uri is not same. Updating from $rememberedMetadata to $metadata",
            )
        } else {
            return
        }

        datastore.edit { preferences ->
            val theSet = getRememberedRepositoriesMetadataFromPreferences(preferences).toMutableMap()

            if (metadata != null) {
                theSet[uri] = metadata
            } else {
                theSet.remove(uri)
            }

            preferences[REMEMBERED_REPOSITORY_METADATA_KEY] =
                theSet.entries
                    .map { Json.encodeToString(it.toPair()) }
                    .toSet()
        }
    }

    private fun getRememberedRepositoriesMetadataFromPreferences(preferences: Preferences) =
        (preferences[REMEMBERED_REPOSITORY_METADATA_KEY] ?: emptyList())
            .map {
                try {
                    Json.decodeFromString<Pair<RepositoryURI, RepositoryMetadata>>(it)
                } catch (e: Throwable) {
                    println("Decode: $e")
                    null
                }
            }.filterNotNull()
            .toMap()
}
