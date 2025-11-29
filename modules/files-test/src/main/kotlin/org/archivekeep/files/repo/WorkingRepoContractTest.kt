package org.archivekeep.files.repo

import io.kotest.assertions.nondeterministic.eventually
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.archivekeep.files.assertLoaded
import org.archivekeep.files.flowToInputStream
import org.archivekeep.files.operations.StatusOperation
import org.archivekeep.files.testContents01
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.withContentsFrom
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.stateIn
import org.junit.jupiter.api.RepeatedTest
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest as standardRunTest

@OptIn(ExperimentalStdlibApi::class)
abstract class WorkingRepoContractTest<T : LocalRepo> {
    interface TestRepo<T : LocalRepo> {
        fun open(testDispatcher: CoroutineDispatcher): T

        fun createUncommittedFile(
            filename: String,
            bytes: ByteArray,
        )
    }

    abstract fun createNew(): TestRepo<T>

    // TODO: `save should not overwrite existing UNCOMMITTED file`

    // TODO: `move should not overwrite existing UNCOMMITTED file`

    @RepeatedTest(4)
    fun `local index loads and updates after save`() =
        runTest {
            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(getDispatcher())
                    .withContentsFrom(testContents01)

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

            eventually(2.seconds) {
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
                    }
                }.flowToInputStream(backgroundScope),
            )

            eventually(2.seconds) {
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
            }

            val undesiredStateEmit = allStates.firstOrNull { it is Loadable.Loaded && it.value.newFiles.isNotEmpty() }
            assert(undesiredStateEmit == null) {
                "Shouldn't ever emit in-progress file as unindexed file, but got: $undesiredStateEmit"
            }
        }

    @RepeatedTest(4)
    fun `local uncommitted file create`() =
        runTest {
            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(getDispatcher())
                    .withContentsFrom(testContents01)

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

            eventually(2.seconds) {
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
            }

            testRepo.createUncommittedFile(
                "BIG_FILE.txt",
                (0..1000000).joinToString(separator = "") { "123456" }.toByteArray(),
            )

            eventually(2.seconds) {
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

    private fun GenericTestScope.getDispatcher() = coroutineContext[CoroutineDispatcher] ?: throw IllegalStateException("Dispatcher expected")

    open fun runTest(testBody: suspend GenericTestScope.() -> Unit) =
        standardRunTest {
            val scope =
                object : GenericTestScope, CoroutineScope by this@standardRunTest {
                    override val backgroundScope = this@standardRunTest.backgroundScope
                }

            with(scope) {
                testBody()
            }
        }
}
