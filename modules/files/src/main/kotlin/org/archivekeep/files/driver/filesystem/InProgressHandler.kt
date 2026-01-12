package org.archivekeep.files.driver.filesystem

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class InProgressHandler<T>(
    scope: CoroutineScope,
    val startDelayDuration: Duration = 100.milliseconds,
    val transform: (value: T) -> String,
) {
    companion object {
        operator fun invoke(
            scope: CoroutineScope,
            startDelayDuration: Duration = 100.milliseconds,
        ) = InProgressHandler<String>(scope, startDelayDuration, transform = { it })
    }

    private val inProgressFiles = MutableStateFlow(emptySet<String>())

    val idleFlagFlow =
        inProgressFiles
            .map { it.isEmpty() }
            .stateIn(scope, SharingStarted.WhileSubscribed(), inProgressFiles.value.isEmpty())

    val jobActiveOnIdle =
        idleFlagFlow.runningFold(null) { previous: CompletableJob?, flag ->
            previous?.cancel()

            if (flag) SupervisorJob() else null
        }.stateIn(scope, SharingStarted.WhileSubscribed(), null)

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

    fun onStart(dstPath: T) {
        inProgressFiles.update { it + setOf(transform(dstPath)) }
    }

    fun onEnd(dstPath: T) {
        inProgressFiles.update { it - setOf(transform(dstPath)) }
    }
}
