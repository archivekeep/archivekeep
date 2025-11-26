package org.archivekeep.utils.io

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WatchKtTest {
    init {
        WatchDefaults.watchDelay = 10.milliseconds
    }

    @RepeatedTest(10)
    fun `list files should work on non-existing directory`(
        repetitionInfo: RepetitionInfo,
        @TempDir t: File,
    ) = runBlocking {
        val futureDir = t.resolve("sub-directory-not-yet-created")

        val collectScope = createCollectScope()

        val emissions = arrayListOf<List<String>?>()
        collectScope.launch {
            futureDir
                .toPath()
                .listFilesFlow { true }
                .map { it?.map { f -> f.name } }
                .distinctUntilChanged()
                .collect { emissions.add(it) }
        }

        eventually(3.seconds) {
            emissions[0] shouldBe null
        }

        delay(5.milliseconds * repetitionInfo.currentRepetition)
        futureDir.mkdirs()
        futureDir.resolve("a-new-file").toPath().writeText("TEST")

        eventually(3.seconds) {
            emissions[emissions.size - 1] shouldBe listOf("a-new-file")
        }

        collectScope.cancel()
    }

    @RepeatedTest(10)
    fun `list files should work on non-existing directory tree`(
        repetitionInfo: RepetitionInfo,
        @TempDir t: File,
    ) = runBlocking {
        val futureDir = t.resolve("sub/sub/sub/tree-not-yet-created")

        val collectScope = createCollectScope()

        val emissions = arrayListOf<List<String>?>()
        collectScope.launch {
            futureDir
                .toPath()
                .listFilesFlow { true }
                .map { it?.map { f -> f.name } }
                .distinctUntilChanged()
                .collect { emissions.add(it) }
        }

        eventually(3.seconds) {
            emissions[0] shouldBe null
        }

        delay(5.milliseconds * repetitionInfo.currentRepetition)
        futureDir.mkdirs()
        futureDir.resolve("a-new-file").toPath().writeText("TEST")

        eventually(3.seconds) {
            emissions[emissions.size - 1] shouldBe listOf("a-new-file")
        }

        collectScope.cancel()
    }

    private fun CoroutineScope.createCollectScope() = CoroutineScope(Job(coroutineContext.job))
}
