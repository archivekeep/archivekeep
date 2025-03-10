package org.archivekeep.utils.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.Watchable
import kotlin.io.path.isDirectory

fun (Path).watchRecursively(
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    log: Boolean = false,
): Flow<List<RecursiveWatchEvent>> =
    flow {
        val watchService =
            withContext(ioDispatcher) {
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
                    .eventFlow(ioDispatcher)
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
                    }.map { (watchable, events) ->
                        val basePath =
                            (watchable as? Path) ?: run {
                                println("Watchable isn't Path: $watchable")
                                return@map emptyList()
                            }

                        events.mapNotNull { event ->
                            val kind = event.kind()

                            when (kind) {
                                StandardWatchEventKinds.OVERFLOW -> RecursiveWatchEvent.Overflow(basePath)

                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_DELETE,
                                StandardWatchEventKinds.ENTRY_MODIFY,
                                -> {
                                    @Suppress("UNCHECKED_CAST")
                                    val kindTyped = kind as WatchEvent.Kind<out Path>

                                    RecursiveWatchEvent.Change(kindTyped, event.context() as Path)
                                }

                                else -> {
                                    println("WARNING: event $kind not supported")
                                    null
                                }
                            }
                        }
                    }

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
fun WatchService.eventFlow(ioDispatcher: CoroutineDispatcher = Dispatchers.IO): Flow<Pair<Watchable, List<WatchEvent<out Any>>>> =
    flow {
        while (currentCoroutineContext().isActive) {
            coroutineScope {
                var key: WatchKey? = null
                val job =
                    launch {
                        runInterruptible(ioDispatcher) {
                            key = take()
                        }
                    }
                job.join()
                val currentKey = key
                if (currentKey != null) {
                    emit(Pair(currentKey.watchable(), currentKey.pollEvents()))
                    currentKey.reset()
                }
            }
        }
    }
