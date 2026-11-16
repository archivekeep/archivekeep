package org.archivekeep.files.procedures.verify

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import org.archivekeep.files.driver.filesystem.files.sqlite.FilesSqliteRepoFactory
import org.archivekeep.files.testContents01
import org.archivekeep.files.utils.runBlockingTest
import org.archivekeep.files.withContentsFrom
import org.archivekeep.files.writeTextKeepTimestamp
import org.archivekeep.utils.loading.Loadable
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test

class VerifyContentsProcedureTest {
    @Test
    fun detectsCorruptedFiles(
        @TempDir archiveDir: File,
    ) = runBlockingTest {
        val repo =
            FilesSqliteRepoFactory
                .create(archiveDir.toPath(), this@runBlockingTest.backgroundScope.coroutineContext.job)
                .withContentsFrom(testContents01)

        // Corrupt some files by changing their content
        archiveDir.resolve("A/01.txt").writeText("corrupted content")
        archiveDir.resolve("B/03.txt").writeText("tampered data")

        val preparation =
            VerifyContentsProcedure()
                .prepare(repo)
                .filterIsInstance<Loadable.Loaded<VerifyContentsProcedure.PreparationResult>>()
                .first()

        preparation.value.filesToVerify shouldBe listOf("A/01.txt", "A/02.txt", "B/03.txt")

        val tracker = VerifyProgressTrackerInMemory()
        preparation.value.execute(repo, null, tracker)

        tracker.verifiedFiles shouldBe setOf("A/02.txt")
        tracker.failedFiles.keys shouldBe setOf("A/01.txt", "B/03.txt")
        tracker.failedFiles["A/01.txt"] shouldContain "Checksum mismatch"
        tracker.failedFiles["B/03.txt"] shouldContain "Checksum mismatch"
    }

    @Test
    fun detectsMissingFiles(
        @TempDir archiveDir: File,
    ) = runBlockingTest {
        val repo =
            FilesSqliteRepoFactory
                .create(archiveDir.toPath(), this@runBlockingTest.backgroundScope.coroutineContext.job)
                .withContentsFrom(testContents01)

        // Delete some files
        archiveDir.resolve("A/01.txt").delete()
        archiveDir.resolve("A/02.txt").delete()

        val preparation =
            VerifyContentsProcedure()
                .prepare(repo)
                .filterIsInstance<Loadable.Loaded<VerifyContentsProcedure.PreparationResult>>()
                .first()

        preparation.value.filesToVerify shouldBe listOf("A/01.txt", "A/02.txt", "B/03.txt")

        val tracker = VerifyProgressTrackerInMemory()
        preparation.value.execute(repo, null, tracker)

        tracker.verifiedFiles shouldBe setOf("B/03.txt")
        tracker.failedFiles.keys shouldBe setOf("A/01.txt", "A/02.txt")
        tracker.failedFiles["A/01.txt"] shouldBe "File not found"
        tracker.failedFiles["A/02.txt"] shouldBe "File not found"
    }

    @Test
    fun detectsSilentCorruptionWithSameSize(
        @TempDir archiveDir: File,
    ) = runBlockingTest {
        val repo =
            FilesSqliteRepoFactory
                .create(archiveDir.toPath(), this@runBlockingTest.backgroundScope.coroutineContext.job)
                .withContentsFrom(testContents01)

        // Simulate bit rot: change content but keep the same size and preserve timestamp
        val fileA = archiveDir.resolve("A/01.txt")

        // Original content is "A/01.txt" (8 bytes). Replace with different 8-byte content.
        fileA.writeTextKeepTimestamp("CORRUPT!") // also 8 bytes

        val preparation =
            VerifyContentsProcedure()
                .prepare(repo)
                .filterIsInstance<Loadable.Loaded<VerifyContentsProcedure.PreparationResult>>()
                .first()

        val tracker = VerifyProgressTrackerInMemory()
        preparation.value.execute(repo, null, tracker)

        // Even though size and timestamp are unchanged, checksum mismatch should be detected
        tracker.verifiedFiles shouldBe setOf("A/02.txt", "B/03.txt")
        tracker.failedFiles.keys shouldBe setOf("A/01.txt")
        tracker.failedFiles["A/01.txt"] shouldContain "Checksum mismatch"
    }

    @Test
    fun detectsSilentCorruptionWithDifferentSize(
        @TempDir archiveDir: File,
    ) = runBlockingTest {
        val repo =
            FilesSqliteRepoFactory
                .create(archiveDir.toPath(), this@runBlockingTest.backgroundScope.coroutineContext.job)
                .withContentsFrom(testContents01)

        // Simulate tampering: change content with different size but preserve timestamp
        val fileB = archiveDir.resolve("B/03.txt")

        // Original content is "B/03.txt" (8 bytes). Replace with longer content.
        fileB.writeTextKeepTimestamp("This file has been tampered with by an attacker")

        val preparation =
            VerifyContentsProcedure()
                .prepare(repo)
                .filterIsInstance<Loadable.Loaded<VerifyContentsProcedure.PreparationResult>>()
                .first()

        val tracker = VerifyProgressTrackerInMemory()
        preparation.value.execute(repo, null, tracker)

        // Checksum mismatch detected regardless of size change
        tracker.verifiedFiles shouldBe setOf("A/01.txt", "A/02.txt")
        tracker.failedFiles.keys shouldBe setOf("B/03.txt")
        tracker.failedFiles["B/03.txt"] shouldContain "Checksum mismatch"
    }

    @Test
    fun allFilesVerifiedWhenUnchanged(
        @TempDir archiveDir: File,
    ) = runBlockingTest {
        val repo =
            FilesSqliteRepoFactory
                .create(archiveDir.toPath(), this@runBlockingTest.backgroundScope.coroutineContext.job)
                .withContentsFrom(testContents01)

        // Don't modify any files

        val preparation =
            VerifyContentsProcedure()
                .prepare(repo)
                .filterIsInstance<Loadable.Loaded<VerifyContentsProcedure.PreparationResult>>()
                .first()

        val tracker = VerifyProgressTrackerInMemory()
        preparation.value.execute(repo, null, tracker)

        tracker.verifiedFiles shouldBe setOf("A/01.txt", "A/02.txt", "B/03.txt")
        tracker.failedFiles shouldBe emptyMap()
    }
}

class VerifyProgressTrackerInMemory : VerifyProgressTracker {
    val verifiedFiles = mutableSetOf<String>()
    val failedFiles = mutableMapOf<String, String>()

    override fun onFileVerified(verifiedFile: String) {
        verifiedFiles.add(verifiedFile)
    }

    override fun onFileVerificationFailed(
        failedFile: String,
        reason: String,
    ) {
        failedFiles[failedFile] = reason
    }
}
