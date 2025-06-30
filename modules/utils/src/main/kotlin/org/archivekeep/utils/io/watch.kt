package org.archivekeep.utils.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun (Path).watch(ioDispatcher: CoroutineDispatcher = Dispatchers.IO): Flow<Any> =
    flow {
        if (!exists()) {
            throw RuntimeException("Directory ${this@watch} doesn't exist")
        }
        if (!isDirectory()) {
            throw RuntimeException("File ${this@watch} is not directory")
        }

        val watchService =
            withContext(ioDispatcher) {
                this@watch.fileSystem.newWatchService()
            }

        try {
            this@watch.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.OVERFLOW,
                StandardWatchEventKinds.ENTRY_DELETE,
            )

            emitAll(
                watchService.eventFlow(),
            )
        } finally {
            withContext(NonCancellable) {
                watchService.close()
            }
        }
    }.flowOn(ioDispatcher)

fun (Path).listFilesFlow(
    rateLimit: Duration = 100.milliseconds,
    repeatDelay: Duration = 250.milliseconds,
    predicate: (file: File) -> Boolean,
) = watch()
    .map { "Change event" }
    .onStart { emit("Get on start") }
    .debounceAndRepeatAfterDelay(
        rateLimit = rateLimit,
        repeatDelay = repeatDelay,
    ).map {
        toFile().listFiles()?.filter(predicate) ?: emptyList()
    }.flowOn(Dispatchers.IO)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
fun <T> (Flow<T>).debounceAndRepeatAfterDelay(
    rateLimit: Duration = 100.milliseconds,
    repeatDelay: Duration = 250.milliseconds,
    mapDelayed: (value: T) -> T = { it },
) = debounce(rateLimit)
    .transformLatest {
        emit(it)
        delay(repeatDelay)
        emit(mapDelayed(it))
    }
