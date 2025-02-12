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
import org.archivekeep.app.core.utils.environment.getRepositoryIndexMemoryDatastorePath
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.mapToOptionalLoadable
import org.archivekeep.app.core.utils.generics.sharedGlobalWhileSubscribed
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.RepoIndex

private val defaultDatastore by lazy {
    PreferenceDataStoreFactory.create(
        corruptionHandler = null,
        migrations = emptyList(),
        produceFile = { getRepositoryIndexMemoryDatastorePath().toFile() },
    )
}

private val REMEMBERED_REPOSITORY_INDEX_KEY = stringSetPreferencesKey("remembered_repository_index")

class MemorizedRepositoryIndexRepositoryInDataStore(
    val datastore: DataStore<Preferences> = defaultDatastore,
) : MemorizedRepositoryIndexRepository {
    val rememberedRepositoriesIndexes =
        datastore.data
            .map(::getRememberedRepositoriesIndexesFromPreferences)
            .onEach {
                println("Loaded repository indexes from data store")
            }.sharedGlobalWhileSubscribed()

    override fun repositoryMemorizedIndexFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepoIndex>> =
        rememberedRepositoriesIndexes
            .mapToOptionalLoadable("Get memorized index for URI: $uri") { it[uri] }

    override suspend fun updateRepositoryMemorizedIndexIfDiffers(
        uri: RepositoryURI,
        accessedIndex: RepoIndex?,
    ) {
        val rememberedIndex = rememberedRepositoriesIndexes.first()[uri]

        if (accessedIndex != rememberedIndex) {
            println("Accessed index and remembered index differ, updating remembered index for $uri")
        } else {
            return
        }

        datastore.edit { preferences ->
            val theSet = getRememberedRepositoriesIndexesFromPreferences(preferences).toMutableMap()

            if (accessedIndex != null) {
                theSet[uri] = accessedIndex
            } else {
                theSet.remove(uri)
            }

            preferences[REMEMBERED_REPOSITORY_INDEX_KEY] =
                theSet.entries
                    .map { Json.encodeToString(it.toPair()) }
                    .toSet()
        }
    }

    private fun getRememberedRepositoriesIndexesFromPreferences(preferences: Preferences) =
        (preferences[REMEMBERED_REPOSITORY_INDEX_KEY] ?: emptyList())
            .map {
                try {
                    Json.decodeFromString<Pair<RepositoryURI, RepoIndex>>(it)
                } catch (e: Throwable) {
                    println("Decode: $e")
                    null
                }
            }.filterNotNull()
            .toMap()
}
