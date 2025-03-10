package org.archivekeep.app.core.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.archivekeep.app.core.utils.generics.singleInstanceWeakValueMap
import kotlin.coroutines.CoroutineContext

class UniqueJobGuard<K : Any, V : UniqueJobGuard.RunnableJob> {
    val stateHoldersWeakReference = singleInstanceWeakValueMap { it: K -> MutableStateFlow<V?>(null) }

    private val ongoingJobsStrongReferences = mutableSetOf<Pair<V, MutableStateFlow<V?>>>()

    fun launch(
        scope: CoroutineScope,
        context: CoroutineContext,
        key: K,
        newJob: V,
    ) {
        val holder = stateHoldersWeakReference[key]
        val newCreated = holder.compareAndSet(null, newJob)

        if (!newCreated) {
            throw IllegalStateException("Already running job")
        }

        val protectPair = Pair(newJob, holder)
        ongoingJobsStrongReferences.add(protectPair)

        scope.launch(context) {
            try {
                newJob.run(coroutineContext.job)
            } finally {
                withContext(NonCancellable) {
                    holder.compareAndSet(newJob, null)
                    ongoingJobsStrongReferences.remove(protectPair)
                }
            }
        }
    }

    interface RunnableJob {
        suspend fun run(job: Job)
    }
}
