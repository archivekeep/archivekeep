package org.archivekeep.files.procedures.sync

import kotlinx.coroutines.runBlocking
import org.archivekeep.files.api.repository.operations.CompareOperation
import org.archivekeep.files.assertRepositoryContents
import org.archivekeep.files.driver.fixtures.FixtureRepoBuilder
import org.archivekeep.files.driver.inmemory.toInMemoryRepo
import org.archivekeep.files.procedures.sync.discovery.RelocationSyncMode
import org.archivekeep.files.procedures.sync.discovery.SyncProcedureDiscovery
import org.archivekeep.files.procedures.sync.job.observation.NoOpSyncExecutionObserver
import org.archivekeep.files.procedures.sync.operations.CopyNewFileOperation
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.utils.hashing.sha256
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
            val prepared = SyncProcedureDiscovery(relocateAll).prepare(baseRepo, otherRepo)

            val job = prepared.createJob(baseRepo, otherRepo, { true }, NoOpSyncExecutionObserver(), selections)

            job.run()

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
                        "2022/02/02.JPG".length.toLong(),
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
                        "old.txt".length.toLong(),
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
