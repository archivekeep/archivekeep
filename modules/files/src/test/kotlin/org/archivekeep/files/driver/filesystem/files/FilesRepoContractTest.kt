package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import org.archivekeep.files.repo.RepoContractTest
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.runBlockingTest
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
            override suspend fun open(testDispatcher: CoroutineDispatcher): FilesRepo =
                FilesRepo(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)
        }
    }

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
