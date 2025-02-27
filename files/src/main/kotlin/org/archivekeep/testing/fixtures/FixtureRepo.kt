package org.archivekeep.testing.fixtures

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.ObservableRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.sha256
import java.io.InputStream

class FixtureRepo(
    val contents: Map<String, String>,
    val uncommittedContents: Map<String, String> = emptyMap(),
) : Repo,
    ObservableRepo {
    private val _index by lazy {
        RepoIndex(
            contents.entries.map {
                RepoIndex.File(
                    path = it.key,
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
        return FixtureRepo(builder.repoContents, builder.repoUncommittedContents)
    }

    override val observable: ObservableRepo = this

    override val indexFlow: Flow<Loadable<RepoIndex>> = MutableStateFlow(_index).mapToLoadable()

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> = flow { TODO() }
}
