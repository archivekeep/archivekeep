package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.archivekeep.files.assertRepositoryContents
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.NewFilesSyncStep.CopyNewFileOperation
import org.archivekeep.files.procedures.sync.RelocationsMoveApplySyncStep.RelocationApplyOperation
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

class SyncProcedureTest {
    private fun testWithSelections(
        selections: Set<SyncOperation>?,
        expectedContents: FixtureRepoBuilder.() -> Unit,
    ) {
        val baseRepo = baseRepoForSyncTest.toInMemoryRepo()
        val otherRepo = otherRepoForSyncTest.toInMemoryRepo()

        runBlocking {
            val prepared = SyncProcedure(relocateAll).prepare(baseRepo, otherRepo)

            prepared.execute(baseRepo, otherRepo, { true }, NoOpLogger(), MutableStateFlow(emptyList()), {}, selections)

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
                CopyNewFileOperation(
                    CompareOperation.Result.ExtraGroup(
                        "2022/02/02.JPG".sha256(),
                        listOf("2022/02/02.JPG"),
                    ),
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
                RelocationApplyOperation(
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
