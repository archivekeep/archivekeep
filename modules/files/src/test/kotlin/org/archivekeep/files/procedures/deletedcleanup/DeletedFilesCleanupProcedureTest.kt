package org.archivekeep.files.procedures.deletedcleanup

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import org.archivekeep.files.driver.filesystem.files.FilesSqliteRepo
import org.archivekeep.files.testContents01
import org.archivekeep.files.utils.runBlockingTest
import org.archivekeep.files.withContentsFrom
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class DeletedFilesCleanupProcedureTest {
    @Test
    fun executeReindexOfAll(
        @TempDir archiveDir: File,
    ) = runBlockingTest {
        val repo =
            FilesSqliteRepo
                .create(archiveDir.toPath(), this@runBlockingTest.backgroundScope.coroutineContext.job)
                .withContentsFrom(testContents01)

        archiveDir.resolve("A/02.txt").delete()
        archiveDir.resolve("B/03.txt").delete()

        eventually(1.seconds) {
            repo.localIndex.firstLoadedOrFailure().missingFiles shouldBe listOf("A/02.txt", "B/03.txt")
        }

        val preparation =
            DeletedFilesCleanupProcedure()
                .prepare(repo)
                .filterIsInstance<Loadable.Loaded<DeletedFilesCleanupProcedure.PreparationResult>>()
                .first()

        preparation.value.missingFiles shouldBe listOf("A/02.txt", "B/03.txt")

        preparation.value.execute(repo, null)

        eventually(1.seconds) {
            repo.localIndex.firstLoadedOrFailure().missingFiles shouldBe emptyList()
        }
    }

    @Test
    fun executeReindexOfSelected(
        @TempDir archiveDir: File,
    ) = runBlockingTest {
        val repo =
            FilesSqliteRepo
                .create(archiveDir.toPath(), this@runBlockingTest.backgroundScope.coroutineContext.job)
                .withContentsFrom(testContents01)

        archiveDir.resolve("A/02.txt").delete()
        archiveDir.resolve("B/03.txt").delete()

        eventually(1.seconds) {
            repo.localIndex.firstLoadedOrFailure().missingFiles shouldBe listOf("A/02.txt", "B/03.txt")
        }

        val preparation =
            DeletedFilesCleanupProcedure()
                .prepare(repo)
                .filterIsInstance<Loadable.Loaded<DeletedFilesCleanupProcedure.PreparationResult>>()
                .first()

        preparation.value.missingFiles shouldBe listOf("A/02.txt", "B/03.txt")

        preparation.value.execute(repo, removeFilesSubsetLimit = setOf("A/02.txt"))

        eventually(1.seconds) {
            repo.localIndex.firstLoadedOrFailure().missingFiles shouldBe listOf("B/03.txt")
        }
    }
}
