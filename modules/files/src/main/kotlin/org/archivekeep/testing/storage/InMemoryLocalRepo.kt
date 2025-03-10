package org.archivekeep.testing.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import org.archivekeep.files.exceptions.FileDoesntExist
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.ObservableWorkingRepo
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.sha256
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

open class InMemoryLocalRepo(
    initialContents: Map<String, ByteArray> = mapOf(),
    initialUnindexedContents: Map<String, ByteArray> = mapOf(),
    metadata: RepositoryMetadata = RepositoryMetadata(),
) : InMemoryRepo(initialContents, metadata),
    LocalRepo,
    ObservableWorkingRepo {
    val unindexedFiles = MutableStateFlow(initialUnindexedContents)

    override suspend fun findAllFiles(globs: List<String>): List<Path> {
        if (globs != listOf("*") && globs != listOf(".")) {
            TODO("Not yet implemented")
        }

        return (contents.keys + unindexedFiles.value.keys).map { Path.of(it) }
    }

    override suspend fun storedFiles(): List<String> =
        this.contents.keys
            .toList()
            .sorted()

    override suspend fun verifyFileExists(path: String): Boolean = contents.containsKey(path)

    override suspend fun fileChecksum(path: String): String = (contents[path] ?: throw FileDoesntExist(path)).sha256()

    override suspend fun computeFileChecksum(path: Path): String =
        (
            contents[path.invariantSeparatorsPathString]
                ?: unindexedFiles.value[path.invariantSeparatorsPathString]
                ?: throw FileDoesntExist(path.toString())
        ).sha256()

    override suspend fun add(path: String) {
        val c = this.unindexedFiles.value[path] ?: throw FileDoesntExist(path)

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

    override suspend fun remove(path: String) {
        TODO("Not yet implemented")
    }

    override val observable: ObservableWorkingRepo
        get() = this

    override val localIndex: Flow<StatusOperation.Result> =
        unindexedFiles
            .transform {
                emit(StatusOperation(listOf("*")).execute(this@InMemoryLocalRepo))
            }
}
