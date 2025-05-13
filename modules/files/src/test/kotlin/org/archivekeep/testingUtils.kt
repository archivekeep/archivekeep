package org.archivekeep

import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import org.archivekeep.testing.fixtures.FixtureRepoBuilder
import org.archivekeep.testing.storage.InMemoryRepo
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.sha256
import org.junit.jupiter.api.assertAll
import java.io.File
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.fail
import kotlin.time.Duration

fun createArchiveWithContents(
    archiveTempDir: File,
    files: Map<String, String>,
) {
    createUnindexedFiles(archiveTempDir, files)

    files.forEach {
        val checksumPath = archiveTempDir.resolve(".archive/checksums").resolve(it.key + ".sha256").toPath()
        checksumPath.createParentDirectories()

        checksumPath.writeText(it.value.sha256())
    }
}

fun createUnindexedFiles(
    archiveTempDir: File,
    files: Map<String, String>,
) {
    files.forEach {
        val filePath = archiveTempDir.resolve(it.key).toPath()
        filePath.createParentDirectories()

        filePath.writeText(it.value)
    }
}

fun assertRepositoryContents(
    actual: InMemoryRepo,
    expected: FixtureRepoBuilder.() -> Unit,
) {
    assertRepositoryContents(
        FixtureRepoBuilder().apply(expected).build().contents,
        actual,
    )
}

fun assertRepositoryContents(
    expected: Map<String, String>,
    actual: InMemoryRepo,
) {
    val expectedContents = expected.mapValues { (_, v) -> v.toByteArray() }

    assertAll(
        {
            val missingFiles = expectedContents.keys - actual.contents.keys
            if (missingFiles.isNotEmpty()) {
                fail("Missing files: $missingFiles")
            }
        },
        {
            val extraFiles = actual.contents.keys - expectedContents.keys
            if (extraFiles.isNotEmpty()) {
                fail("Extra files: $extraFiles")
            }
        },
        {
            assertAll(
                expectedContents.map { (ek, ev) ->
                    {
                        actual.contents[ek].let { ac ->
                            if (ac != null && !ac.contentEquals(ev)) {
                                fail("Different content for: $ek - ${java.lang.String(ac)} differs to ${String(ev)}")
                            }
                        }
                    }
                },
            )
        },
    )
}

fun TestCoroutineScheduler.advanceTimeByAndWaitForIdle(delayTime: Duration) {
    advanceTimeBy(delayTime)
    advanceUntilIdle()
}

fun TestScope.advanceTimeByAndWaitForIdle(delayTime: Duration) {
    this.testScheduler.advanceTimeByAndWaitForIdle(delayTime)
}

fun <T> (Loadable<T>).assertLoaded(contentsAssert: (value: T) -> Unit) {
    assert(this is Loadable.Loaded) { "Not loaded" }
    contentsAssert((this as Loadable.Loaded).value)
}
