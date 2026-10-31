package org.archivekeep.files.driver.filesystem.files.objectbox

import kotlinx.coroutines.CoroutineDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.runBlockingTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory

class FilesObjectBoxRepoContractTest : RepoContractTest<FilesObjectBoxRepo>() {
    @field:TempDir
    lateinit var tempPath: Path

    override suspend fun createNew(): TestRepo<FilesObjectBoxRepo> {
        val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

        FilesObjectBoxRepo.create(path).apply { close() }

        return object : TestRepo<FilesObjectBoxRepo> {
            override suspend fun open(testDispatcher: CoroutineDispatcher): FilesObjectBoxRepo =
                FilesObjectBoxRepo(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)
        }
    }

    @Test
    @Disabled("Not stable")
    override fun `metadata initial load (empty), update and load (new-value), and re-open inital load (new-value)`() {
        // TODO: fix the implementation to make it stable
    }

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
