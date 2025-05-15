package org.archivekeep.files.repo.files

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class InProgressHandler(
    scope: CoroutineScope,
    val startDelayDuration: Duration = 100.milliseconds,
) {
    private val lock: Lock = ReentrantLock(true)

    private val inProgressFiles = MutableStateFlow(emptySet<String>())

    val jobActiveOnIdle = MutableStateFlow<Job?>(SupervisorJob())
    val jobActiveOnIdleDelayedStart =
        jobActiveOnIdle
            .onEach {
                if (it != null) {
                    // don't be too eager to start, because repo could be in a little time window
                    // just after the end of previous one, and just before the start of the next one
                    delay(startDelayDuration)
                    currentCoroutineContext().ensureActive()
                }
            }

    fun onStart(dstPath: Path) {
        lock.withLock {
            inProgressFiles.update { it + setOf(dstPath.absolutePathString()) }
            jobActiveOnIdle.updateAndGet { null }?.cancel()
        }
    }

    fun onEnd(dstPath: Path) {
        lock.withLock {
            val newValues = inProgressFiles.updateAndGet { it - setOf(dstPath.absolutePathString()) }

            if (newValues.isEmpty()) {
                jobActiveOnIdle.value = SupervisorJob()
            }
        }
    }
}
