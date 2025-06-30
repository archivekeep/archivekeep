package org.archivekeep.files.repo.files

import kotlinx.coroutines.test.TestDispatcher
import org.archivekeep.files.repo.WorkingRepoContractTest
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory
import kotlin.io.path.outputStream

class FilesWorkingRepoContractTest : WorkingRepoContractTest<FilesRepo>() {
    @field:TempDir
    lateinit var tempPath: Path

    override fun createNew(): TestRepo<FilesRepo> =
        object : TestRepo<FilesRepo> {
            val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

            override fun open(testDispatcher: TestDispatcher): FilesRepo = FilesRepo(path, stateDispatcher = testDispatcher, ioDispatcher = testDispatcher)

            override fun createUncommittedFile(
                filename: String,
                bytes: ByteArray,
            ) {
                path
                    .resolve(filename)
                    .outputStream()
                    .use { it.write(bytes) }
            }
        }
}
