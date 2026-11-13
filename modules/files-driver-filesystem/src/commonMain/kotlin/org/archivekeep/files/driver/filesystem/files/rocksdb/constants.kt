package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal val rocksDbFlushDelay = 250.milliseconds

object RocksDBDefaults {
    var filesChangedDebounce: Duration = 3.seconds
}
