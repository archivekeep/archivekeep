package org.archivekeep.utils.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchService
import java.nio.file.Watchable
import kotlin.io.path.isDirectory
import kotlin.time.Duration.Companion.milliseconds

object WatchDefaults {
    var watchDelay = 1500.milliseconds
}

fun (Path).watchRecursively(
    ioDispatcher: CoroutineDispatcher,
    pollDispatcher: CoroutineDispatcher,
    log: Boolean = false,
): Flow<List<CustomWatchEvent>> =
    flow {
        val watchService =
            withContext(ioDispatcher) {
                // TODO: reuse watch services
                fileSystem.newWatchService()
            }

        try {
            suspend fun (Path).register() {
                withContext(ioDispatcher) {
                    register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.OVERFLOW,
                        StandardWatchEventKinds.ENTRY_DELETE,
                    )
                }

                if (log) {
                    println("File watch: registered: $this")
                }
            }

            suspend fun (Path).registerRecursive() {
                this@registerRecursive.register()

                this@registerRecursive.toFile().walk().forEach {
                    if (it.isDirectory) {
                        it.toPath().register()
                    }
                }
            }

            this@watchRecursively.registerRecursive()

            val selfUpdatingFlow =
                watchService
                    .eventFlow(pollDispatcher)
                    .onEach { (watchable, events) ->
                        val basePath =
                            (watchable as? Path) ?: run {
                                println("ERROR: Watchable isn't Path: $watchable")
                                return@onEach
                            }

                        events.forEach { event ->
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                val modifiedPath = (event.context() as Path)

                                val path = basePath.resolve(modifiedPath)

                                if (path.isDirectory()) {
                                    if (log) {
                                        println("File watch: ENTRY_CREATE for directory: $path: adding to watch")
                                    }

                                    path.register()
                                }
                            } else if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                                if (log) {
                                    println("File watch: OVERFLOW for $basePath, re-adding recursively")
                                }
                                basePath.registerRecursive()
                            }
                        }
                    }.mapToCustomWatchEvents()

            emit(listOf(CustomWatchEvent.Started))
            emitAll(selfUpdatingFlow)
        } finally {
            withContext(NonCancellable) {
                watchService.close()
            }
        }
    }

/**
 * Creates a flow WatchEvent from a watchService
 */
fun WatchService.eventFlow(pollDispatcher: CoroutineDispatcher = Dispatchers.IO): Flow<Pair<Watchable, List<WatchEvent<out Any>>>> =
    flow {
        while (currentCoroutineContext().isActive) {
            // TODO: switch back to runInterruptible { take() } when ensured this won't exhaust dispatcher's threads
            val currentKey = poll()

            if (currentKey != null) {
                emit(Pair(currentKey.watchable(), currentKey.pollEvents()))
                currentKey.reset()
            } else {
                delay(WatchDefaults.watchDelay)
            }
        }
    }.flowOn(pollDispatcher)
