package org.archivekeep.app.core.persistence.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.archivekeep.app.core.utils.environment.getRepositoryMetadataMemoryDatastorePath
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.mapToOptionalLoadable
import org.archivekeep.app.core.utils.generics.sharedGlobalWhileSubscribed
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.core.repo.RepositoryMetadata

private val defaultDatastore by lazy {
    PreferenceDataStoreFactory.create(
        corruptionHandler = null,
        migrations = emptyList(),
        produceFile = { getRepositoryMetadataMemoryDatastorePath().toFile() },
    )
}

private val REMEMBERED_REPOSITORY_METADATA_KEY = stringSetPreferencesKey("remembered_repository_metadata")

class MemorizedRepositoryMetadataRepositoryInDataStore(
    val datastore: DataStore<Preferences> = defaultDatastore,
) : MemorizedRepositoryMetadataRepository {
    val rememberedRepositoriesMetadata =
        datastore.data
            .map(::getRememberedRepositoriesMetadataFromPreferences)
            .onEach {
                println("Loaded repository metadata: $it")
            }.sharedGlobalWhileSubscribed()

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
