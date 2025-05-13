package org.archivekeep.files.repo

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.archivekeep.advanceTimeByAndWaitForIdle
import org.archivekeep.assertLoaded
import org.archivekeep.populateTestContents01
import org.archivekeep.quickSave
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.sha256
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

abstract class RepoContractTest<T : Repo> {
    abstract fun createNew(testDispatcher: TestDispatcher): T

    @Test
    fun `index loads and updates after save`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo =
                createNew(dispatcher)
                    .apply { populateTestContents01() }

            val indexFlowState =
                testRepo
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

            testRepo.quickSave("C/04.txt")

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
}
