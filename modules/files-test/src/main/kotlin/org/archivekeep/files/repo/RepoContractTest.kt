package org.archivekeep.files.repo

import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.archivekeep.files.assertLoaded
import org.archivekeep.files.exceptions.ChecksumMismatch
import org.archivekeep.files.exceptions.DestinationExists
import org.archivekeep.files.quickSave
import org.archivekeep.files.shouldHaveCommittedContentsOf
import org.archivekeep.files.testContents01
import org.archivekeep.files.withContentsFrom
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.sha256
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
abstract class RepoContractTest<T : Repo> {
    interface TestRepo<T : Repo> {
        suspend fun open(testDispatcher: TestDispatcher): T
    }

    abstract suspend fun createNew(): TestRepo<T>

    @Test
    fun `save should store file`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val repoAccessor = testRepo.open(dispatcher)

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

    @Test
    fun `save should not overwrite existing file`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val repoAccessor = testRepo.open(dispatcher)

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

    @Test
    fun `save should not store wrong file, and should work on correct retry`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val repoAccessor = testRepo.open(dispatcher)

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

            // TODO: eventual consistency is not ideal for the index() call afterwards,
            //       fix implementation(s) to have inner invalidation, and wait for correct value
            advanceUntilIdle()

            assertEquals(
                listOf(
                    RepoIndex.File.forStringContents("test-file.txt", "RIGHT CONTENTS"),
                ),
                repoAccessor.index().files,
            )
        }

    @Test
    fun `move file`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val repoAccessor =
                testRepo
                    .open(dispatcher)
                    .withContentsFrom(testContents01)

            repoAccessor.move(
                from = "A/01.txt",
                to = "NEW/A/01.txt",
            )

            advanceUntilIdle()

            assertEquals(
                listOf(
                    RepoIndex.File.forStringContents(path = "A/02.txt"),
                    RepoIndex.File.forStringContents(path = "B/03.txt"),
                    RepoIndex.File.forStringContents(path = "NEW/A/01.txt", stringContents = "A/01.txt"),
                ),
                repoAccessor.index().files,
            )
        }

    @Test
    fun `move should not overwrite existing file`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val repoAccessor =
                testRepo
                    .open(dispatcher)
                    .withContentsFrom(testContents01)

            assertThrows<DestinationExists> {
                repoAccessor.move(
                    from = "A/01.txt",
                    to = "A/02.txt",
                )
            }

            advanceUntilIdle()

            assertEquals(
                listOf(
                    RepoIndex.File.forStringContents(path = "A/01.txt"),
                    RepoIndex.File.forStringContents(path = "A/02.txt"),
                    RepoIndex.File.forStringContents(path = "B/03.txt"),
                ),
                repoAccessor.index().files,
            )
        }

    @Test
    fun `index loads and updates after save`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(dispatcher)
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

    @Test
    open fun `metadata initial load (empty), update and load (new-value), and re-open inital load (new-value)`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val newID = UUID.randomUUID().toString()

            run {
                val repoAccessor =
                    testRepo
                        .open(dispatcher)
                        .withContentsFrom(testContents01)

                val collectScope = CoroutineScope(SupervisorJob())

                val metadataFlowState =
                    repoAccessor
                        .metadataFlow
                        .stateIn(collectScope, SharingStarted.Eagerly)

                eventually(2.seconds) {
                    metadataFlowState.value.assertLoaded {
                        assertEquals(null, it.associationGroupId)
                    }
                }

                repoAccessor.updateMetadata {
                    it.copy(associationGroupId = newID)
                }

                eventually(2.seconds) {
                    metadataFlowState.value.assertLoaded {
                        assertEquals(newID, it.associationGroupId)
                    }
                }

                collectScope.cancel()
            }

            run {
                val repoAccessor = testRepo.open(dispatcher)

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

    @Test
    fun `metadata change should not affect contents`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val newID = UUID.randomUUID().toString()

            val repoAccessor =
                testRepo
                    .open(dispatcher)
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
}
