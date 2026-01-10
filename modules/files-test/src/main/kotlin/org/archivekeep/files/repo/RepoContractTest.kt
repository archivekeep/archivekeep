package org.archivekeep.files.repo

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import org.archivekeep.files.assertLoaded
import org.archivekeep.files.exceptions.ChecksumMismatch
import org.archivekeep.files.exceptions.DestinationExists
import org.archivekeep.files.quickSave
import org.archivekeep.files.shouldHaveCommittedContentsOf
import org.archivekeep.files.testContents01
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.standardRunTest
import org.archivekeep.files.withContentsFrom
import org.archivekeep.utils.hashing.sha256
import org.archivekeep.utils.loading.stateIn
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
abstract class RepoContractTest<T : Repo> {
    interface TestRepo<T : Repo> {
        suspend fun open(testDispatcher: CoroutineDispatcher): T
    }

    abstract suspend fun createNew(): TestRepo<T>

    @RepeatedTest(4)
    fun `save should store file`() =
        runTest {
            val testRepo = createNew()
            val repoAccessor = testRepo.open(this@runTest.ioDispatcher)

            repoAccessor.quickSave(
                "test-file.txt",
                "TEST CONTENTS",
            )

            assertEquals(
                listOf(
                    RepoIndex.File.forStringContents("test-file.txt", "TEST CONTENTS"),
                ),
                repoAccessor.index().files,
            )

            repoAccessor.assertFileHasStringContents(
                "test-file.txt",
                "TEST CONTENTS",
            )
        }

    @RepeatedTest(4)
    fun `save should not overwrite existing file`() =
        runTest {
            val testRepo = createNew()
            val repoAccessor = testRepo.open(this@runTest.ioDispatcher)

            repoAccessor.quickSave(
                "test-file.txt",
                "ORIGINAL CONTENTS",
            )

            assertThrows<DestinationExists> {
                repoAccessor.quickSave(
                    "test-file.txt",
                    "NEW CONTENTS",
                )
            }

            assertEquals(
                listOf(
                    RepoIndex.File.forStringContents("test-file.txt", "ORIGINAL CONTENTS"),
                ),
                repoAccessor.index().files,
            )

            repoAccessor.assertFileHasStringContents(
                "test-file.txt",
                "ORIGINAL CONTENTS",
            )
        }

    @RepeatedTest(4)
    fun `save should not store wrong file, and should work on correct retry`() =
        runTest {
            val testRepo = createNew()
            val repoAccessor = testRepo.open(this@runTest.ioDispatcher)

            assertThrows<ChecksumMismatch> {
                repoAccessor.save(
                    "test-file.txt",
                    ArchiveFileInfo(
                        "WRONG CONTENTS".length.toLong(),
                        "RIGHT CONTENTS".sha256(),
                    ),
                    "WRONG CONTENTS".byteInputStream(),
                )
            }

            assertEquals(emptyList(), repoAccessor.index().files)

            repoAccessor.quickSave(
                "test-file.txt",
                "RIGHT CONTENTS",
            )

            eventually(3.seconds) {
                repoAccessor.index().files shouldBe
                    listOf(
                        RepoIndex.File.forStringContents("test-file.txt", "RIGHT CONTENTS"),
                    )
            }
        }

    @RepeatedTest(4)
    fun `move file`() =
        runTest {
            val testRepo = createNew()
            val repoAccessor =
                testRepo
                    .open(this@runTest.ioDispatcher)
                    .withContentsFrom(testContents01)

            repoAccessor.move(
                from = "A/01.txt",
                to = "NEW/A/01.txt",
            )

            eventually(3.seconds) {
                repoAccessor.index().files shouldBe
                    listOf(
                        RepoIndex.File.forStringContents(path = "A/02.txt"),
                        RepoIndex.File.forStringContents(path = "B/03.txt"),
                        RepoIndex.File.forStringContents(path = "NEW/A/01.txt", stringContents = "A/01.txt"),
                    )
            }
        }

    @RepeatedTest(4)
    fun `move should not overwrite existing file`() =
        runTest {
            val testRepo = createNew()
            val repoAccessor =
                testRepo
                    .open(this@runTest.ioDispatcher)
                    .withContentsFrom(testContents01)

            assertThrows<DestinationExists> {
                repoAccessor.move(
                    from = "A/01.txt",
                    to = "A/02.txt",
                )
            }

            eventually(3.seconds) {
                repoAccessor.index().files shouldBe
                    listOf(
                        RepoIndex.File.forStringContents(path = "A/01.txt"),
                        RepoIndex.File.forStringContents(path = "A/02.txt"),
                        RepoIndex.File.forStringContents(path = "B/03.txt"),
                    )
            }
        }

    @RepeatedTest(4)
    fun `index loads and updates after save`() =
        runTest {
            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(this@runTest.ioDispatcher)
                    .withContentsFrom(testContents01)

            val indexFlowState =
                repoAccessor
                    .indexFlow
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            eventually(2.seconds) {
                indexFlowState.value.assertLoaded {
                    assertEquals(
                        listOf(
                            RepoIndex.File.forStringContents(path = "A/01.txt"),
                            RepoIndex.File.forStringContents(path = "A/02.txt"),
                            RepoIndex.File.forStringContents(path = "B/03.txt"),
                        ),
                        it.files,
                    )
                }
            }

            repoAccessor.quickSave("C/04.txt")

            eventually(2.seconds) {
                indexFlowState.value.assertLoaded {
                    assertEquals(
                        listOf(
                            RepoIndex.File.forStringContents(path = "A/01.txt"),
                            RepoIndex.File.forStringContents(path = "A/02.txt"),
                            RepoIndex.File.forStringContents(path = "B/03.txt"),
                            RepoIndex.File.forStringContents(path = "C/04.txt"),
                        ),
                        it.files,
                    )
                }
            }
        }

    @RepeatedTest(4)
    open fun `metadata initial load (empty), update and load (new-value), and re-open inital load (new-value)`() =
        runTest {
            val testRepo = createNew()
            val newID = UUID.randomUUID().toString()

            run {
                val repoAccessor =
                    testRepo
                        .open(this@runTest.ioDispatcher)
                        .withContentsFrom(testContents01)

                val collectScope = CoroutineScope(SupervisorJob())

                val metadataFlowState =
                    repoAccessor
                        .metadataFlow
                        .stateIn(collectScope, SharingStarted.Eagerly)

                eventually(5.seconds) {
                    metadataFlowState.value.assertLoaded {
                        assertEquals(null, it.associationGroupId)
                    }
                }

                repoAccessor.updateMetadata {
                    it.copy(associationGroupId = newID)
                }

                eventually(5.seconds) {
                    metadataFlowState.value.assertLoaded {
                        assertEquals(newID, it.associationGroupId)
                    }
                }

                collectScope.cancel()
            }

            run {
                val repoAccessor = testRepo.open(this@runTest.ioDispatcher)

                val collectScope = CoroutineScope(SupervisorJob())

                val metadataFlowState =
                    repoAccessor
                        .metadataFlow
                        .stateIn(collectScope, SharingStarted.Eagerly)

                eventually(2.seconds) {
                    metadataFlowState.value.assertLoaded {
                        assertEquals(newID, it.associationGroupId)
                    }
                }

                collectScope.cancel()
            }
        }

    @RepeatedTest(4)
    fun `metadata change should not affect contents`() =
        runTest {
            val testRepo = createNew()
            val newID = UUID.randomUUID().toString()

            val repoAccessor =
                testRepo
                    .open(this@runTest.ioDispatcher)
                    .withContentsFrom(testContents01)

            repoAccessor.updateMetadata {
                it.copy(
                    associationGroupId = newID,
                )
            }

            assertEquals(
                repoAccessor.getMetadata().associationGroupId,
                newID,
            )

            repoAccessor shouldHaveCommittedContentsOf testContents01
        }

    open fun runTest(testBody: suspend GenericTestScope.() -> Unit) = standardRunTest(testBody)
}
