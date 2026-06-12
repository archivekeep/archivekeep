package org.archivekeep.files.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.files.eventuallyShouldBeLoadedAndValueShouldBe
import org.archivekeep.files.flowToInputStream
import org.archivekeep.files.testContents01
import org.archivekeep.files.testContents01InitialWorkingStatus
import org.archivekeep.files.utils.GenericTestScope
import org.archivekeep.files.utils.standardRunTest
import org.archivekeep.files.withContentsFrom
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.stateIn
import org.junit.jupiter.api.RepeatedTest

@OptIn(ExperimentalStdlibApi::class)
abstract class WorkingRepoContractTest<T : LocalRepo> {
    interface TestRepo<T : LocalRepo> {
        fun open(
            scope: GenericTestScope,
            testDispatcher: CoroutineDispatcher,
        ): T

        fun createUncommittedFile(
            filename: String,
            bytes: ByteArray,
        )

        fun overwriteFile(
            filename: String,
            bytes: ByteArray,
            preserveTimestamp: Boolean = false,
        )

        fun deleteFile(filename: String)
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
                    .open(this@runTest, getDispatcher())
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

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe testContents01InitialWorkingStatus

            repoAccessor.save(
                "BIG_FILE.txt",
                ArchiveFileInfo(
                    (20 * (1024 * 128) * "12345678".length).toLong(),
                    "5f148ac95d8f76f453d151c9d6608a5922f12a4b9fe041e809342d4c4b9e4aeb",
                ),
                flow {
                    for (i in 0..<20) {
                        emit((0..<(1024 * 128)).joinToString("") { "12345678" }.toByteArray())
                    }
                }.flowToInputStream(backgroundScope),
            )

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
                StatusOperation.Result(
                    newFiles = emptyList(),
                    indexedFiles =
                        listOf(
                            "A/01.txt",
                            "A/02.txt",
                            "B/03.txt",
                            "BIG_FILE.txt",
                        ),
                    modifiedIndexedFiles = emptyList(),
                    missingFiles = emptyList(),
                )

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
                    .open(this@runTest, getDispatcher())
                    .withContentsFrom(testContents01)

            val indexFlowState =
                repoAccessor
                    .localIndex
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe testContents01InitialWorkingStatus

            testRepo.createUncommittedFile(
                "BIG_FILE.txt",
                (0..1000000).joinToString(separator = "") { "123456" }.toByteArray(),
            )

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
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
                    modifiedIndexedFiles = emptyList(),
                    missingFiles = emptyList(),
                )
        }

    @RepeatedTest(4)
    fun shouldDetectDeleteAndSupportRemove() =
        runTest {
            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(this@runTest, getDispatcher())
                    .withContentsFrom(testContents01)

            val indexFlowState =
                repoAccessor
                    .localIndex
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe testContents01InitialWorkingStatus

            testRepo.deleteFile("A/02.txt")

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
                StatusOperation.Result(
                    newFiles = emptyList(),
                    indexedFiles =
                        listOf(
                            "A/01.txt",
                            "B/03.txt",
                        ),
                    modifiedIndexedFiles = emptyList(),
                    missingFiles = listOf("A/02.txt"),
                )

            repoAccessor.remove("A/02.txt")

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
                StatusOperation.Result(
                    newFiles = emptyList(),
                    indexedFiles =
                        listOf(
                            "A/01.txt",
                            "B/03.txt",
                        ),
                    modifiedIndexedFiles = emptyList(),
                    missingFiles = emptyList(),
                )
        }

    @RepeatedTest(4)
    open fun shouldDetectTimestampModificationOfIndexedFileAndSupportReAdd() =
        runTest {
            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(this@runTest, getDispatcher())
                    .withContentsFrom(testContents01)

            val indexFlowState =
                repoAccessor
                    .localIndex
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe testContents01InitialWorkingStatus

            testRepo.overwriteFile("A/02.txt", "A/99.txt".toByteArray())

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
                StatusOperation.Result(
                    newFiles = emptyList(),
                    indexedFiles =
                        listOf(
                            "A/01.txt",
                            "A/02.txt",
                            "B/03.txt",
                        ),
                    modifiedIndexedFiles =
                        listOf(
                            "A/02.txt",
                        ),
                    missingFiles = emptyList(),
                )

            repoAccessor.add("A/02.txt", reindex = true)

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
                StatusOperation.Result(
                    newFiles = emptyList(),
                    indexedFiles =
                        listOf(
                            "A/01.txt",
                            "A/02.txt",
                            "B/03.txt",
                        ),
                    modifiedIndexedFiles = emptyList(),
                    missingFiles = emptyList(),
                )
        }

    @RepeatedTest(4)
    open fun shouldDetectSizeModificationOfIndexedFileAndSupportReAdd() =
        runTest {
            val testRepo = createNew()

            val repoAccessor =
                testRepo
                    .open(this@runTest, getDispatcher())
                    .withContentsFrom(testContents01)

            val indexFlowState =
                repoAccessor
                    .localIndex
                    .stateIn(backgroundScope, SharingStarted.Eagerly)

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe testContents01InitialWorkingStatus

            testRepo.overwriteFile(
                "A/02.txt",
                (0..1000000).joinToString(separator = "") { "123456" }.toByteArray(),
                preserveTimestamp = true,
            )

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
                StatusOperation.Result(
                    newFiles = emptyList(),
                    indexedFiles =
                        listOf(
                            "A/01.txt",
                            "A/02.txt",
                            "B/03.txt",
                        ),
                    modifiedIndexedFiles = listOf("A/02.txt"),
                    missingFiles = emptyList(),
                )

            repoAccessor.add("A/02.txt", reindex = true)

            indexFlowState eventuallyShouldBeLoadedAndValueShouldBe
                StatusOperation.Result(
                    newFiles = emptyList(),
                    indexedFiles =
                        listOf(
                            "A/01.txt",
                            "A/02.txt",
                            "B/03.txt",
                        ),
                    modifiedIndexedFiles = emptyList(),
                    missingFiles = emptyList(),
                )
        }

    private fun GenericTestScope.getDispatcher() = coroutineContext[CoroutineDispatcher] ?: throw IllegalStateException("Dispatcher expected")

    open fun runTest(testBody: suspend GenericTestScope.() -> Unit) = standardRunTest(testBody)
}
