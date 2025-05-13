package org.archivekeep.files.operations

import kotlinx.coroutines.runBlocking
import org.archivekeep.files.assertRepositoryContents
import org.archivekeep.files.operations.sync.NewFilesSyncStep.CopyNewFileSubOperation
import org.archivekeep.files.operations.sync.RelocationSyncMode
import org.archivekeep.files.operations.sync.RelocationsMoveApplySyncStep.RelocationApplySubOperation
import org.archivekeep.files.operations.sync.SyncLogger
import org.archivekeep.files.operations.sync.SyncOperation
import org.archivekeep.files.operations.sync.SyncSubOperation
import org.archivekeep.testing.fixtures.FixtureRepoBuilder
import org.archivekeep.testing.storage.toInMemoryRepo
import org.archivekeep.utils.sha256
import org.junit.jupiter.api.Test

val relocateAll = RelocationSyncMode.Move(allowDuplicateIncrease = true, allowDuplicateReduction = true)

val baseRepoForSyncTest =
    FixtureRepoBuilder()
        .apply {
            addStored("2022/02/01.JPG")
            addStored("2022/02/02.JPG")
            addStored("2022/02/03.JPG")
            addStored("2022/02/04.JPG")
            addStored("new.txt", "old.txt")
        }.build()

val otherRepoForSyncTest =
    FixtureRepoBuilder()
        .apply {
            addStored("2022/02/01.JPG")
            addStored("old.txt")
        }.build()

class SyncOperationTest {
    private fun testWithSelections(
        selections: Set<SyncSubOperation>?,
        expectedContents: FixtureRepoBuilder.() -> Unit,
    ) {
        val baseRepo = baseRepoForSyncTest.toInMemoryRepo()
        val otherRepo = otherRepoForSyncTest.toInMemoryRepo()

        runBlocking {
            val prepared = SyncOperation(relocateAll).prepare(baseRepo, otherRepo)

            prepared.execute(baseRepo, otherRepo, { true }, NoOpLogger(), {}, selections)

            assertRepositoryContents(otherRepo, expectedContents)
        }
    }

    @Test
    fun `should apply all new files and moves`() {
        testWithSelections(null) {
            addStored("2022/02/01.JPG")
            addStored("2022/02/02.JPG")
            addStored("2022/02/03.JPG")
            addStored("2022/02/04.JPG")
            addStored("new.txt", "old.txt")
        }
    }

    @Test
    fun `should create one new selected file`() {
        testWithSelections(
            setOf(
                CopyNewFileSubOperation(
                    CompareOperation.Result.ExtraGroup("2022/02/02.JPG".sha256(), listOf("2022/02/02.JPG")),
                ),
            ),
        ) {
            addFrom(otherRepoForSyncTest)
            addStored("2022/02/02.JPG")
        }
    }

    @Test
    fun `should create one selected move`() {
        testWithSelections(
            setOf(
                RelocationApplySubOperation(
                    CompareOperation.Result.Relocation(
                        "old.txt".sha256(),
                        baseFilenames = listOf("new.txt"),
                        otherFilenames = listOf("old.txt"),
                    ),
                ),
            ),
        ) {
            addFrom(otherRepoForSyncTest)
            deletePattern("old.txt".toRegex())
            addStored("new.txt", "old.txt")
        }
    }
}

class NoOpLogger : SyncLogger {
    override fun onFileStored(filename: String) {
    }

    override fun onFileMoved(
        from: String,
        to: String,
    ) {
    }

    override fun onFileDeleted(filename: String) {
    }
}
