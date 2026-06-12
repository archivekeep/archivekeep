package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.job
import org.archivekeep.files.repo.WorkingRepoContractTest
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.runBlockingTest
import org.archivekeep.utils.io.WatchDefaults
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.milliseconds

class FilesSqliteWorkingRepoContractTest : WorkingRepoContractTest<FilesSqliteRepo>() {
    init {
        WatchDefaults.watchDelay = 10.milliseconds
    }

    @field:TempDir
    lateinit var tempPath: Path

    override fun createNew(): TestRepo<FilesSqliteRepo> =
        object : TestRepo<FilesSqliteRepo> {
            val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

            override fun open(
                scope: GenericTestScope,
                testDispatcher: CoroutineDispatcher,
            ): FilesSqliteRepo =
                FilesSqliteRepo(
                    path,
                    parentJob = scope.backgroundScope.coroutineContext.job,
                    stateDispatcher = testDispatcher,
                    ioDispatcher = testDispatcher,
                )

            override fun createUncommittedFile(
                filename: String,
                bytes: ByteArray,
            ) {
                path
                    .resolve(filename)
                    .outputStream()
                    .use { it.write(bytes) }
            }

            override fun overwriteFile(
                filename: String,
                bytes: ByteArray,
                preserveTimestamp: Boolean,
            ) {
                path
                    .resolve(filename)
                    .outputStream()
                    .use { it.write(bytes) }
            }

            override fun deleteFile(filename: String) {
                path.resolve(filename).deleteIfExists()
            }
        }

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
