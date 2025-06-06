package org.archivekeep.files.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.archivekeep.files.RepositoryAssociationGroupId
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.utils.loading.Loadable
import java.io.InputStream
import java.nio.file.Path

@Serializable
data class RepoIndex(
    val files: List<File>,
) {
    @Transient
    val byChecksumSha256 = files.groupBy { it.checksumSha256 }

    @Transient
    val byPath = files.associateBy { it.path }

    @Serializable
    data class File(
        val path: String,
        val size: Long?,
        val checksumSha256: String,
    )
}

data class ArchiveFileInfo(
    val length: Long,
    val checksumSha256: String,
) {
    companion object
}

@Serializable
data class RepositoryAssociationGroup(
    val id: RepositoryAssociationGroupId,
    val name: String,
)

@Serializable
data class RepositoryMetadata(
    val associationGroupId: RepositoryAssociationGroupId? = null,
)

interface Repo {
    val indexFlow: StateFlow<Loadable<RepoIndex>>
    val metadataFlow: Flow<Loadable<RepositoryMetadata>>

    suspend fun index(): RepoIndex

    suspend fun move(
        from: String,
        to: String,
    )

    suspend fun <T> open(
        filename: String,
        block: suspend (ArchiveFileInfo, InputStream) -> T,
    ): T

    suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit = {},
    )

    suspend fun delete(filename: String)

    suspend fun getMetadata(): RepositoryMetadata

    suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata)
}

interface LocalRepo : Repo {
    val localIndex: Flow<Loadable<StatusOperation.Result>>

    suspend fun contains(path: String): Boolean

    suspend fun findAllFiles(globs: List<String>): List<Path>

    suspend fun indexedFilenames(): List<String>

    suspend fun verifyFileExists(path: String): Boolean

    suspend fun fileChecksum(path: String): String

    suspend fun computeFileChecksum(path: Path): String

    suspend fun add(path: String)

    suspend fun remove(path: String)

    fun getFileSize(filename: String): Long?
}
