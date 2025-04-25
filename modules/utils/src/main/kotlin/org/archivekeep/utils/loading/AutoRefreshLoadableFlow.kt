package org.archivekeep.utils.loading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AutoRefreshLoadableFlow<T>(
    val scope: CoroutineScope,
    val dispatcher: CoroutineDispatcher,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(100),
    val updateTriggerFlow: Flow<Any>,
    val loadFn: suspend () -> T,
) {
    private val loadingMutex = Mutex()
    private val mutableStateFlow = MutableStateFlow<Loadable<T>>(Loadable.Loading)

    suspend fun getFreshAndUpdateState(): T =
        withContext(dispatcher) {
            loadingMutex.withLock {
                mutableStateFlow.produceAndGet {
                    loadFn()
                }
            }
        }

    val stateFlow: StateFlow<Loadable<T>> =
        mutableStateFlow.let {
            val updateCollector =
                scope.launch(
                    dispatcher,
                    start = CoroutineStart.LAZY,
                ) {
                    updateTriggerFlow.collect {
                        try {
                            getFreshAndUpdateState()
                        } catch (_: Throwable) {
                            // ignore - gets set by produceAndGet
                        }
                    }
                }

            it
                .onStart { updateCollector.start() }
                .onCompletion { updateCollector.cancel() }
                .stateIn(scope, sharingStarted)
        }
}
