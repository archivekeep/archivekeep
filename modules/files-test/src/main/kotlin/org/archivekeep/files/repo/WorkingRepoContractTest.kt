package org.archivekeep.files.repo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.archivekeep.files.advanceTimeByAndWaitForIdle
import org.archivekeep.files.assertLoaded
import org.archivekeep.files.flowToInputStream
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.files.populateTestContents01
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.stateIn
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

abstract class WorkingRepoContractTest<T : LocalRepo> {
    interface TestRepo<T : LocalRepo> {
        fun open(testDispatcher: TestDispatcher): T

        fun createUncommittedFile(
            filename: String,
            bytes: ByteArray,
        )
    }

    abstract fun createNew(): TestRepo<T>

    // TODO: `save should not overwrite existing UNCOMMITTED file`

    // TODO: `move should not overwrite existing UNCOMMITTED file`

    @Test
    fun `local index loads and updates after save`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(dispatcher)
                    .apply { populateTestContents01() }

            val indexFlowState =
                repoAccessor
                    .localIndex
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            var allStates = listOf(indexFlowState.value)
            backgroundScope.launch {
                repoAccessor
                    .localIndex
                    .collect {
                        allStates = allStates + listOf(it)
                    }
            }

            advanceTimeByAndWaitForIdle(2.seconds)

            indexFlowState.value.assertLoaded {
                assertEquals(
                    StatusOperation.Result(
                        newFiles = emptyList(),
                        indexedFiles =
                            listOf(
                                "A/01.txt",
                                "A/02.txt",
                                "B/03.txt",
                            ),
                    ),
                    it,
                )
            }

            repoAccessor.save(
                "BIG_FILE.txt",
                ArchiveFileInfo(
                    (20 * (1024 * 128) * "12345678".length).toLong(),
                    "6da17e0f6175d7754a7a9c8f3b531f6f503b4fa4c8271588a787c0de50e9b6b9",
                ),
                flow {
                    for (i in 0..20) {
                        emit((0..(1024 * 128)).joinToString("") { "12345678" }.toByteArray())
                        delay(10.milliseconds)
                    }
                }.flowToInputStream(backgroundScope),
            )

            advanceTimeByAndWaitForIdle(2.seconds)

            indexFlowState.value.assertLoaded {
                assertEquals(
                    StatusOperation.Result(
                        newFiles = emptyList(),
                        indexedFiles =
                            listOf(
                                "A/01.txt",
                                "A/02.txt",
                                "B/03.txt",
                                "BIG_FILE.txt",
                            ),
                    ),
                    it,
                )
            }

            val undesiredStateEmit = allStates.firstOrNull { it is Loadable.Loaded && it.value.newFiles.isNotEmpty() }
            assert(undesiredStateEmit == null) {
                "Shouldn't ever emit in-progress file as unindexed file, but got: $undesiredStateEmit"
            }
        }

    @Test
    fun `local uncommitted file create`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)

            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(dispatcher)
                    .apply { populateTestContents01() }

            val indexFlowState =
                repoAccessor
                    .localIndex
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            var allStates = listOf(indexFlowState.value)
            backgroundScope.launch {
                repoAccessor
                    .localIndex
                    .collect {
                        println("Adding: $it")
                        allStates = allStates + listOf(it)
                    }
            }

            advanceTimeByAndWaitForIdle(2.seconds)

            indexFlowState.value.assertLoaded {
                assertEquals(
                    StatusOperation.Result(
                        newFiles = emptyList(),
                        indexedFiles =
                            listOf(
                                "A/01.txt",
                                "A/02.txt",
                                "B/03.txt",
                            ),
                    ),
                    it,
                )
            }

            withContext(dispatcher) {
                testRepo.createUncommittedFile(
                    "BIG_FILE.txt",
                    (0..1000000).joinToString(separator = "") { "123456" }.toByteArray(),
                )
            }

            advanceTimeByAndWaitForIdle(2.minutes)

            indexFlowState.value.assertLoaded {
                assertEquals(
                    StatusOperation.Result(
                        newFiles =
                            listOf(
                                "BIG_FILE.txt",
                            ),
                        indexedFiles =
                            listOf(
                                "A/01.txt",
                                "A/02.txt",
                                "B/03.txt",
                            ),
                    ),
                    it,
                )
            }
        }
}
