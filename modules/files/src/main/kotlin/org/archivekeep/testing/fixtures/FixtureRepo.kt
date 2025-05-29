package org.archivekeep.testing.fixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.sha256
import java.io.InputStream

class FixtureRepo(
    val contents: Map<String, String>,
    val uncommittedContents: Map<String, String> = emptyMap(),
    val missingContents: Map<String, String> = emptyMap(),
) : Repo {
    constructor(builder: FixtureRepoBuilder) : this(builder.repoContents, builder.repoUncommittedContents, builder.repoMissingContents)

    constructor(fn: (FixtureRepoBuilder).() -> Unit) : this(FixtureRepoBuilder().also(fn))

    val _index by lazy {
        RepoIndex(
            contents.entries.map {
                RepoIndex.File(
                    path = it.key,
                    size = it.value.length.toLong(),
                    checksumSha256 = it.value.sha256(),
                )
            } +
                missingContents.entries.map {
                    RepoIndex.File(
                        path = it.key,
                        size = it.value.length.toLong(),
                        checksumSha256 = it.value.sha256(),
                    )
                },
        )
    }

    override suspend fun index(): RepoIndex = _index

    override suspend fun move(
        from: String,
        to: String,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun open(filename: String): Pair<ArchiveFileInfo, InputStream> {
        TODO("Not yet implemented")
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(filename: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getMetadata(): RepositoryMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        TODO("Not yet implemented")
    }

    fun derive(modifications: FixtureRepoBuilder.() -> Unit): FixtureRepo {
        val builder = FixtureRepoBuilder(contents, uncommittedContents)
        builder.modifications()
        return builder.build()
    }

    override val indexFlow = MutableStateFlow(Loadable.Loaded(_index))

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> = flow { TODO() }
}
