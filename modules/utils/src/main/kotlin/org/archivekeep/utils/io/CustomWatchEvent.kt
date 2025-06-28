package org.archivekeep.utils.io

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.Watchable

sealed interface CustomWatchEvent {
    data class Overflow(
        val baseDir: Path,
    ) : CustomWatchEvent

    data class Change(
        val watchEventKind: WatchEvent.Kind<out Path>,
        val subject: Path,
    ) : CustomWatchEvent
}

fun (Flow<Pair<Watchable, List<WatchEvent<out Any>>>>).mapToCustomWatchEvents(): Flow<List<CustomWatchEvent>> =
    this
        .map { (watchable, events) ->
            val basePath =
                (watchable as? Path) ?: run {
                    println("Watchable isn't Path: $watchable")
                    return@map emptyList()
                }

            events.mapNotNull { event ->
                val kind = event.kind()

                when (kind) {
                    StandardWatchEventKinds.OVERFLOW -> CustomWatchEvent.Overflow(basePath)

                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    -> {
                        @Suppress("UNCHECKED_CAST")
                        val kindTyped = kind as WatchEvent.Kind<out Path>

                        val modifiedPath = (event.context() as Path)
                        val path = basePath.resolve(modifiedPath)

                        CustomWatchEvent.Change(kindTyped, path)
                    }

                    else -> {
                        println("WARNING: event $kind not supported")
                        null
                    }
                }
            }
        }
