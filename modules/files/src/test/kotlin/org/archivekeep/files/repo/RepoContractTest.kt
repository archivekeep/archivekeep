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
import kotlin.time.Duration.Companion.milliseconds

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
                    .observable
                    .indexFlow
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            advanceTimeByAndWaitForIdle(100.milliseconds)

            indexFlowState.value.assertLoaded {
                assertEquals(
                    listOf(
                        RepoIndex.File(path = "A/01.txt", checksumSha256 = "A/01.txt".sha256()),
                        RepoIndex.File(path = "A/02.txt", checksumSha256 = "A/02.txt".sha256()),
                        RepoIndex.File(path = "B/03.txt", checksumSha256 = "B/03.txt".sha256()),
                    ),
                    it.files,
                )
            }

            repoAccessor.quickSave("C/04.txt")

            advanceTimeByAndWaitForIdle(100.milliseconds)

            indexFlowState.value.assertLoaded {
                assertEquals(
                    listOf(
                        RepoIndex.File(path = "A/01.txt", checksumSha256 = "A/01.txt".sha256()),
                        RepoIndex.File(path = "A/02.txt", checksumSha256 = "A/02.txt".sha256()),
                        RepoIndex.File(path = "B/03.txt", checksumSha256 = "B/03.txt".sha256()),
                        RepoIndex.File(path = "C/04.txt", checksumSha256 = "C/04.txt".sha256()),
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
                        .observable
                        .metadataFlow
                        .stateIn(backgroundScope, SharingStarted.Eagerly)

                advanceTimeByAndWaitForIdle(100.milliseconds)

                metadataFlowState.value.assertLoaded {
                    assertEquals(null, it.associationGroupId)
                }

                repoAccessor.updateMetadata {
                    it.copy(associationGroupId = newID)
                }

                advanceTimeByAndWaitForIdle(100.milliseconds)

                metadataFlowState.value.assertLoaded {
                    assertEquals(newID, it.associationGroupId)
                }
            }

            run {
                val repoAccessor = testRepo.open(dispatcher)

                val metadataFlowState =
                    repoAccessor
                        .observable
                        .metadataFlow
                        .stateIn(backgroundScope, SharingStarted.Eagerly)

                advanceTimeByAndWaitForIdle(100.milliseconds)

                metadataFlowState.value.assertLoaded {
                    assertEquals(newID, it.associationGroupId)
                }
            }
        }
}
