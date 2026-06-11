package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.job
import org.archivekeep.files.repo.WorkingRepoContractTest
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.runBlockingTest
import org.archivekeep.utils.io.WatchDefaults
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectory
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.outputStream
import kotlin.io.path.setLastModifiedTime
import kotlin.test.Test
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

            override fun overwriteFile(
                filename: String,
                bytes: ByteArray,
                preserveTimestamp: Boolean,
            ) {
                val oldTimestamp = if (preserveTimestamp) path.getLastModifiedTime() else null

                path
                    .resolve(filename)
                    .outputStream()
                    .use { it.write(bytes) }

                oldTimestamp?.let { path.setLastModifiedTime(oldTimestamp) }
            }
        }

    @Test
    @Disabled("Feature not supported - not possible with checksum-only index")
    override fun shouldDetectTimestampModificationOfIndexedFileAndSupportReAdd() {
        super.shouldDetectTimestampModificationOfIndexedFileAndSupportReAdd()
    }

    @Test
    @Disabled("Feature not supported - not possible with checksum-only index")
    override fun shouldDetectSizeModificationOfIndexedFileAndSupportReAdd() {
        super.shouldDetectSizeModificationOfIndexedFileAndSupportReAdd()
    }

    override fun runTest(testBody: suspend GenericTestScope.() -> Unit) = runBlockingTest(testBody)
}
