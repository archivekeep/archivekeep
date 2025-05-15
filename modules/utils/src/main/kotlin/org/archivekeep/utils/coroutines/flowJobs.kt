package org.archivekeep.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> (Flow<Job?>).flowScopedToThisJob(flowProducer: () -> Flow<T>): Flow<T> =
    this
        .transformLatest { job ->
            if (job != null) {
                val channel = Channel<T>()

                val collectJob =
                    CoroutineScope(currentCoroutineContext() + job).launch {
                        flowProducer().collect { channel.send(it) }
                    }

                try {
                    // this should abandon anything if something starts changing on the filesystem for the repo
                    emitAll(channel)
                } finally {
                    collectJob.cancel()
                }
            }
        }
