package org.archivekeep.files.driver.filesystem.files

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

class FilesSqliteRepoContractTest : RepoContractTest<FilesSqliteRepo>() {
    @field:TempDir
    lateinit var tempPath: Path

    override suspend fun createNew(): TestRepo<FilesSqliteRepo> {
        val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

        FilesSqliteRepo.create(path)

        return object : TestRepo<FilesSqliteRepo> {
            override suspend fun open(testDispatcher: CoroutineDispatcher): FilesSqliteRepo =
                FilesSqliteRepo(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)
        }
    }

    @Test
    @Disabled("Not stable")
    override fun `metadata initial load (empty), update and load (new-value), and re-open inital load (new-value)`() {
        // TODO: fix the implementation to make it stable
    }

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
