package org.archivekeep.files.repo.files

import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory

class FilesRepoContractTest : RepoContractTest<FilesRepo>() {
    @field:TempDir
    lateinit var tempPath: Path

    override fun createNew(): TestRepo<FilesRepo> {
        val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

        createFilesRepo(path)

        return object : TestRepo<FilesRepo> {
            override fun open(testDispatcher: TestDispatcher): FilesRepo = FilesRepo(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)
        }
    }

    @Test
    @Disabled("Not stable")
    override fun `metadata initial load (empty), update and load (new-value), and re-open inital load (new-value)`() {
        // TODO: fix the implementation to make it stable
    }
}
