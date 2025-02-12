package org.archivekeep.testing.storage

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import org.archivekeep.files.exceptions.DestinationExists
import org.archivekeep.files.exceptions.FileDoesntExist
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.ObservableRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.Loadable
import org.archivekeep.utils.sha256
import java.io.InputStream

open class InMemoryRepo(
    initialContents: Map<String, ByteArray> = mapOf(),
    metadata: RepositoryMetadata = RepositoryMetadata(),
) : Repo,
    ObservableRepo {
    val contentsFlow = MutableStateFlow(initialContents)

    val contents: Map<String, ByteArray>
        get() = contentsFlow.value

    suspend fun contains(path: String): Boolean = contents.containsKey(path)

    override val indexFlow: Flow<RepoIndex> =
        contentsFlow
            .map { contents ->
                RepoIndex(
                    contents.entries.map {
                        RepoIndex.File(
                            path = it.key,
                            checksumSha256 = it.value.sha256(),
                        )
                    },
                )
            }.shareIn(GlobalScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val _metadataFlow = MutableStateFlow(metadata)

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        _metadataFlow.map { Loadable.Loaded(it) }

    override suspend fun index(): RepoIndex = indexFlow.first()

    override suspend fun move(
        from: String,
        to: String,
    ) {
        if (contains(to)) {
            throw FileDoesntExist(to)
        }

        contentsFlow.update {
            val contents = it.toMutableMap()

            contents[to] = contents[from] ?: throw FileDoesntExist(from)
            contents.remove(from)

            contents
        }
    }

    override suspend fun open(filename: String): Pair<ArchiveFileInfo, InputStream> {
        val c = contents[filename] ?: throw FileDoesntExist(filename)

        return Pair(
            ArchiveFileInfo(
                length = c.size.toLong(),
                checksumSha256 = c.sha256(),
            ),
            c.inputStream(),
        )
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
    ) {
        if (contents.containsKey(filename)) {
            throw DestinationExists(filename)
        }

        val c = stream.readAllBytes()

        if (c.sha256() != info.checksumSha256) {
            throw RuntimeException("Invalid data")
        }

        contentsFlow.update {
            it + mapOf(filename to c)
        }
    }

    override suspend fun getMetadata(): RepositoryMetadata = _metadataFlow.first()

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        _metadataFlow.update { transform(it) }
    }

    override val observable: ObservableRepo = this
}
