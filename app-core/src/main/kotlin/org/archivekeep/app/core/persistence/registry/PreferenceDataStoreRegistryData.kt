package org.archivekeep.app.core.persistence.registry

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.archivekeep.app.core.utils.environment.getRegistryDatastorePath
import org.archivekeep.app.core.utils.generics.sharedWhileSubscribed
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.loading.mapToLoadable

private val defaultDatastore by lazy {
    PreferenceDataStoreFactory.create(
        corruptionHandler = null,
        migrations = emptyList(),
        produceFile = { getRegistryDatastorePath().toFile() },
    )
}

private val REGISTERED_REPO_KEY = stringSetPreferencesKey("registered_repositories")
private val REGISTERED_FS_STORAGE_KEY = stringSetPreferencesKey("registered_filesystem_storages")

class PreferenceDataStoreRegistryData(
    val scope: CoroutineScope,
    val datastore: DataStore<Preferences> = defaultDatastore,
) : RegistryDataStore {
    override val registeredRepositories =
        datastore.data
            .map(::getRepositoriesFromPreferences)
            .onEach {
                println("Loaded repositories: $it")
            }.sharedWhileSubscribed(scope)

    override val registeredStorages =
        datastore.data
            .mapToLoadable(transform = ::getStoragesFromPreferences)
            .onEach {
                println("Loaded file system storages: $it")
            }.sharedWhileSubscribed(scope)

    override suspend fun updateStorage(
        uri: StorageURI,
        transform: (storage: RegisteredStorage) -> RegisteredStorage,
    ) {
        updateFileSystemStorages { storages ->
            var updated = false

            val newStorages =
                storages
                    .map {
                        if (it.uri == uri) {
                            updated = true
                            transform(it)
                        } else {
                            it
                        }
                    }.toSet()

            if (updated) {
                newStorages
            } else {
                newStorages + setOf(transform(RegisteredStorage(uri)))
            }
        }
    }

    override suspend fun updateRepositories(fn: (old: Set<RegisteredRepository>) -> Set<RegisteredRepository>) {
        datastore.edit { preferences ->
            val old = getRepositoriesFromPreferences(preferences)
            val new = fn(old).map { Json.encodeToString(it) }.toSet()

            preferences[REGISTERED_REPO_KEY] = new
        }
    }

    suspend fun updateFileSystemStorages(fn: (old: Set<RegisteredStorage>) -> Set<RegisteredStorage>) {
        datastore.edit { preferences ->
            val old = getStoragesFromPreferences(preferences)
            val new = fn(old).map { Json.encodeToString(it) }.toSet()

            preferences[REGISTERED_FS_STORAGE_KEY] = new
        }
    }

    private fun getRepositoriesFromPreferences(preferences: Preferences) =
        (preferences[REGISTERED_REPO_KEY] ?: emptyList())
            .map {
                Json.decodeFromString<RegisteredRepository>(it)
            }.toSet()

    private fun getStoragesFromPreferences(preferences: Preferences) =
        (preferences[REGISTERED_FS_STORAGE_KEY] ?: emptyList())
            .map {
                Json.decodeFromString<RegisteredStorage>(it)
            }.toSet()
}
