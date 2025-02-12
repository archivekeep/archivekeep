package org.archivekeep.utils.io

import java.nio.file.Path
import java.nio.file.WatchEvent

sealed interface RecursiveWatchEvent {
    class Overflow(
        val baseDir: Path,
    ) : RecursiveWatchEvent

    class Change(
        val watchEventKind: WatchEvent.Kind<out Path>,
        val subject: Path,
    ) : RecursiveWatchEvent
}
