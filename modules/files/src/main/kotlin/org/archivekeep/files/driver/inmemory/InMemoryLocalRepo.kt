package org.archivekeep.files.driver.inmemory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.archivekeep.files.api.exceptions.FileDoesntExist
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.files.driver.fixtures.FixtureRepo
import org.archivekeep.files.procedures.indexupdate.RepoPerformanceTracker
import org.archivekeep.utils.hashing.sha256
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import java.nio.file.Path

open class InMemoryLocalRepo(
    initialContents: Map<String, ByteArray> = mapOf(),
    initialUnindexedContents: Map<String, ByteArray> = mapOf(),
    initialMissingContents: Map<String, ByteArray> = mapOf(),
    metadata: RepositoryMetadata = RepositoryMetadata(),
) : InMemoryRepo(initialContents, initialMissingContents, metadata),
    LocalRepo {
    val unindexedFiles = MutableStateFlow(initialUnindexedContents)

    override suspend fun findAllFiles(globs: List<String>): List<Path> {
        if (globs != listOf("*") && globs != listOf(".")) {
            TODO("Not yet implemented")
        }

        return (contents.keys + unindexedFiles.value.keys).map { Path.of(it) }
    }

    override suspend fun indexedFilenames(): List<String> =
        (this.contents.keys + this.missingContentsFlow.value.keys)
            .toList()
            .sorted()

    override suspend fun verifyFileExists(path: String): Boolean = contents.containsKey(path)

    override suspend fun fileChecksum(path: String): String = (contents[path] ?: missingContentsFlow.value[path] ?: throw FileDoesntExist(path)).sha256()

    override suspend fun computeFileChecksum(
        path: String,
        performanceTracker: RepoPerformanceTracker,
    ): String =
        (
            contents[path]
                ?: unindexedFiles.value[path]
                ?: throw FileDoesntExist(path)
        ).sha256()

    override suspend fun prepareAdd(
        path: String,
        performanceTracker: RepoPerformanceTracker,
    ): LocalRepo.PreparedAdd {
        val c = this.unindexedFiles.value[path] ?: throw FileDoesntExist(path)

        return object : LocalRepo.PreparedAdd {
            override val checksumSha256 = c.sha256()

            override suspend fun add(reindex: Boolean) {
                contentsFlow.update {
                    it + mapOf(path to c)
                }
                unindexedFiles.update {
                    it
                        .toMutableMap()
                        .apply { remove(path) }
                        .toMap()
                }
            }
        }
    }

    override suspend fun remove(
        path: String,
        performanceTracker: RepoPerformanceTracker,
    ) {
        val c = this.contentsFlow.value[path]

        if (c != null) {
            unindexedFiles.update {
                it + mapOf(path to c)
            }
            contentsFlow.update {
                it
                    .toMutableMap()
                    .apply { remove(path) }
                    .toMap()
            }
        } else {
            if (missingContentsFlow.value[path] == null) {
                throw FileDoesntExist(path)
            }

            missingContentsFlow.update {
                it
                    .toMutableMap()
                    .apply { remove(path) }
                    .toMap()
            }
        }
    }

    override fun getFileSize(filename: String): Long? =
        contentsFlow.value[filename]?.size?.toLong()
            ?: unindexedFiles.value[filename]?.size?.toLong()

    override val localIndex: Flow<Loadable<StatusOperation.Result>> =
        unindexedFiles
            .mapToLoadable {
                StatusOperation(listOf("*")).execute(this@InMemoryLocalRepo)
            }
}

fun FixtureRepo.toInMemoryLocalRepo(): InMemoryLocalRepo =
    InMemoryLocalRepo(
        this.contents.mapValues { (_, v) -> v.toByteArray() },
        this.uncommittedContents.mapValues { (_, v) -> v.toByteArray() },
        this.missingContents.mapValues { (_, v) -> v.toByteArray() },
    )
