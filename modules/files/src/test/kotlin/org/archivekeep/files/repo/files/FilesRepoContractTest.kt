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

    override fun createNew(): TestRepo<FilesRepo> {
        return object : TestRepo<FilesRepo> {
            val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

            override fun open(testDispatcher: TestDispatcher): FilesRepo {
                return FilesRepo(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)
            }
        }
    }
}
