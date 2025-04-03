package org.archivekeep.app.desktop

import java.nio.file.Path
import kotlin.io.path.Path

fun getRegistryDatastorePath(): Path = Path("${getArchiveKeepDataDir()}/registry.preferences_pb")

fun getRepositoryMetadataMemoryDatastorePath(): Path = Path("${getArchiveKeepDataDir()}/repository_metadata_memory.preferences_pb")

fun getRepositoryIndexMemoryDatastorePath(): Path = Path("${getArchiveKeepDataDir()}/repository_index_memory.preferences_pb")

fun getWalletDatastorePath(): Path = Path("${getArchiveKeepDataDir()}/credentials.jwe")

private fun getXDGBaseDataDir(): String {
    val allEnvs = System.getenv()

    val stateDirBase =
        allEnvs["XDG_DATA_HOME"]
            ?: allEnvs["HOME"]?.let { "$it/.local/share" }
            ?: throw RuntimeException("Neither XDG_DATA_HOME nor HOME is set")

    return stateDirBase
}

private fun getArchiveKeepDataDir(): String = "${getXDGBaseDataDir()}/archivekeep"
