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
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.milliseconds

class FilesWorkingRepoContractTest : WorkingRepoContractTest<FilesRepo>() {
    init {
        WatchDefaults.watchDelay = 10.milliseconds
    }

    @field:TempDir
    lateinit var tempPath: Path

    override fun createNew(): TestRepo<FilesRepo> =
        object : TestRepo<FilesRepo> {
            val path = tempPath.resolve(UUID.randomUUID().toString()).createDirectory()

            override fun open(
                scope: GenericTestScope,
                testDispatcher: CoroutineDispatcher,
            ): FilesRepo =
                FilesRepo(
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
        }

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
