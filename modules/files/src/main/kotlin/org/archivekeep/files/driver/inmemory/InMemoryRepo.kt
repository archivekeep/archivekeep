package org.archivekeep.files.driver.inmemory

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.archivekeep.files.api.exceptions.ChecksumMismatch
import org.archivekeep.files.api.exceptions.DestinationExists
import org.archivekeep.files.api.exceptions.FileDoesntExist
import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.driver.fixtures.FixtureRepo
import org.archivekeep.utils.hashing.sha256
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.stateIn
import java.io.InputStream

open class InMemoryRepo(
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    protected val _metadataFlow: MutableStateFlow<RepositoryMetadata>,
    protected val contentsFlow: MutableStateFlow<Map<String, ByteArray>>,
    protected val missingContentsFlow: MutableStateFlow<Map<String, ByteArray>>,
) : Repo {
    constructor(
        initialContents: Map<String, ByteArray> = mapOf(),
        missingContents: Map<String, ByteArray> = mapOf(),
        metadata: RepositoryMetadata = RepositoryMetadata(),
        stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : this(stateDispatcher, MutableStateFlow(metadata), MutableStateFlow(initialContents), MutableStateFlow(missingContents))

    private val scope = CoroutineScope(SupervisorJob() + stateDispatcher)

    val contents: Map<String, ByteArray>
        get() = contentsFlow.value

    suspend fun contains(path: String): Boolean = contents.containsKey(path)

    override val indexFlow =
        combine(
            contentsFlow,
            missingContentsFlow,
        ) { a, b ->
            a + b
        }.mapToLoadable { contents ->
            RepoIndex(
                contents.entries.map {
                    RepoIndex.File(
                        path = it.key,
                        size = it.value.size.toLong(),
                        checksumSha256 = it.value.sha256(),
                    )
                },
            )
        }.stateIn(scope)

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        _metadataFlow.map { Loadable.Loaded(it) }

    override suspend fun index(): RepoIndex = indexFlow.firstLoadedOrFailure()

    override suspend fun move(
        from: String,
        to: String,
    ) {
        if (contains(to)) {
            throw DestinationExists(to)
        }

        contentsFlow.update {
            val contents = it.toMutableMap()

            contents[to] = contents[from] ?: throw FileDoesntExist(from)
            contents.remove(from)

            contents
        }
    }

    override suspend fun delete(filename: String) {
        if (contains(filename)) {
            throw FileDoesntExist(filename)
        }

        contentsFlow.update { it - filename }
    }

    override suspend fun <T> open(
        filename: String,
        block: suspend (ArchiveFileInfo, InputStream) -> T,
    ): T {
        val c = contents[filename] ?: throw FileDoesntExist(filename)

        return block(
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
        monitor: (copiedBytes: Long) -> Unit,
    ) {
        if (contents.containsKey(filename)) {
            throw DestinationExists(filename)
        }

        val c = stream.readAllBytes()
        val actualChecksum = c.sha256()

        if (actualChecksum != info.checksumSha256) {
            throw ChecksumMismatch(expected = info.checksumSha256, actual = actualChecksum)
        }

        contentsFlow.update {
            it + mapOf(filename to c)
        }
    }

    override suspend fun getMetadata(): RepositoryMetadata = _metadataFlow.first()

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        _metadataFlow.update { transform(it) }
    }
}

fun FixtureRepo.toInMemoryRepo(): InMemoryRepo = InMemoryRepo(this.contents.mapValues { (_, v) -> v.toByteArray() })
