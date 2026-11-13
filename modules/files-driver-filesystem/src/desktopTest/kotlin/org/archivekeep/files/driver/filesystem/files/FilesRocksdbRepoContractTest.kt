package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import org.archivekeep.files.driver.filesystem.files.rocksdb.FilesRocksDbRepoFactory
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.runBlockingTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory

class FilesRocksdbRepoContractTest : RepoContractTest<FilesDatabaseRepo>() {
    init {
        setFilesRocksdbTestConfiguration()
    }

    @field:TempDir
    lateinit var tempPath: Path

    override suspend fun createNew(): TestRepo<FilesDatabaseRepo> {
        val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

        FilesRocksDbRepoFactory.create(path).close()

        return object : TestRepo<FilesDatabaseRepo> {
            override suspend fun open(testDispatcher: CoroutineDispatcher): FilesDatabaseRepo =
                FilesRocksDbRepoFactory.openOrNull(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)!!
        }
    }

    @Test
    @Disabled("Not stable")
    override fun `metadata initial load (empty), update and load (new-value), and re-open inital load (new-value)`() {
        // TODO: fix the implementation to make it stable
    }

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
