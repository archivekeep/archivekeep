package org.archivekeep.utils.loading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ResourceLoader<T>(
    val scope: CoroutineScope,
    val dispatcher: CoroutineDispatcher,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(100),
    val enabledFlow: StateFlow<Boolean> = MutableStateFlow(true),
    val updateTriggerFlow: Flow<Any>,
    val observe: (T) -> Unit = { },
    val loadFn: suspend () -> T,
) {
    private val loadingMutex = Mutex()

    private val mutableStateFlow = MutableStateFlow<Loadable<T>>(Loadable.Loading)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val updateCollection =
        flow<Unit> {
            enabledFlow
                .runningFold(null) { previous: Boolean?, next: Boolean ->
                    if (previous != null && next) {
                        delay(500.milliseconds)
                    }

                    next
                }.collectLatest { enabled ->
                    if (enabled != true) {
                        return@collectLatest
                    }

                    updateTriggerFlow
                        .onStart { emit("start") }
                        .debounce { 500.milliseconds }
                        .conflate()
                        .collect {
                            try {
                                getFreshAndUpdateState()
                            } catch (_: Throwable) {
                                // ignore - gets set by produceAndGet
                            }
                        }
                }
        }.shareIn(
            scope = scope,
            started =
                SharingStarted.WhileSubscribed(
                    stopTimeout = Duration.ZERO,
                    replayExpiration = Duration.ZERO,
                ),
            replay = 0,
        )

    suspend fun getFreshAndUpdateState(): T =
        withContext(dispatcher) {
            loadingMutex.withLock {
                mutableStateFlow.produceAndGet {
                    loadFn()
                }.also {
                    observe(it)
                }
            }
        }

    val stateFlow: StateFlow<Loadable<T>> =
        channelFlow {
            val updateCollector =
                launch(start = CoroutineStart.LAZY) {
                    updateCollection.collect {
                        // collect it infinitely so it keeps running as long as the data flow is active.
                    }
                }

            mutableStateFlow
                .onStart { updateCollector.start() }
                .onCompletion { updateCollector.cancel() }
                .collect { send(it) }
        }.stateIn(scope)
}
