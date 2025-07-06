package org.archivekeep.files.repo.files

import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory

class FilesRepoContractTest : RepoContractTest<FilesRepo>() {
    @field:TempDir
    lateinit var tempPath: Path

    override suspend fun createNew(): TestRepo<FilesRepo> {
        val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

        FilesRepo.create(path)

        return object : TestRepo<FilesRepo> {
            override suspend fun open(testDispatcher: TestDispatcher): FilesRepo =
                FilesRepo(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)
        }
    }
}
