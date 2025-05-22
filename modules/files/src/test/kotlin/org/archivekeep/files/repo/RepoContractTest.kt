package org.archivekeep.files.repo

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.archivekeep.files.advanceTimeByAndWaitForIdle
import org.archivekeep.files.assertLoaded
import org.archivekeep.files.populateTestContents01
import org.archivekeep.files.quickSave
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.sha256
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

abstract class RepoContractTest<T : Repo> {
    interface TestRepo<T : Repo> {
        fun open(testDispatcher: TestDispatcher): T
    }

    abstract fun createNew(): TestRepo<T>

    @Test
    fun `index loads and updates after save`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(dispatcher)
                    .apply { populateTestContents01() }

            val indexFlowState =
                repoAccessor
                    .indexFlow
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            advanceTimeByAndWaitForIdle(2.seconds)

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

            repoAccessor.quickSave("C/04.txt")

            advanceTimeByAndWaitForIdle(2.seconds)

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

    @Test
    fun `metadata initial load (empty), update and load (new-value), and re-open inital load (new-value)`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()
            val newID = UUID.randomUUID().toString()

            run {
                val repoAccessor =
                    testRepo
                        .open(dispatcher)
                        .apply { populateTestContents01() }

                val metadataFlowState =
                    repoAccessor
                        .metadataFlow
                        .stateIn(backgroundScope, SharingStarted.Eagerly)

                advanceTimeByAndWaitForIdle(2.seconds)

                metadataFlowState.value.assertLoaded {
                    assertEquals(null, it.associationGroupId)
                }

                repoAccessor.updateMetadata {
                    it.copy(associationGroupId = newID)
                }

                advanceTimeByAndWaitForIdle(2.seconds)

                metadataFlowState.value.assertLoaded {
                    assertEquals(newID, it.associationGroupId)
                }
            }

            run {
                val repoAccessor = testRepo.open(dispatcher)

                val metadataFlowState =
                    repoAccessor
                        .metadataFlow
                        .stateIn(backgroundScope, SharingStarted.Eagerly)

                advanceTimeByAndWaitForIdle(2.seconds)

                metadataFlowState.value.assertLoaded {
                    assertEquals(newID, it.associationGroupId)
                }
            }
        }
}

private fun RepoIndex.File.Companion.forStringContents(
    path: String,
    stringContents: String = path,
): RepoIndex.File =
    RepoIndex.File(
        path = path,
        size = stringContents.length.toLong(),
        checksumSha256 = stringContents.sha256(),
    )
