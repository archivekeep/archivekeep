package org.archivekeep.app.android

import java.io.File

class AndroidEnvironmentPaths(
    val dataDir: File,
) {
    fun getRegistryDatastoreFile() = dataDir.resolve("registry.preferences_pb")

    fun getWalletDatastoreFile() = dataDir.resolve("credentials.jwe")

    fun getRepositoryMetadataMemoryDatastoreFile() = dataDir.resolve("repository_metadata_memory.preferences_pb")

    fun getRepositoryIndexMemoryDatastoreFile() = dataDir.resolve("repository_index_memory.preferences_pb")
}
