package org.archivekeep.files.driver.filesystem.files

import org.archivekeep.files.driver.filesystem.files.rocksdb.RocksDBDefaults
import org.archivekeep.utils.io.WatchDefaults
import kotlin.time.Duration.Companion.milliseconds

fun setFilesRocksdbTestConfiguration() {
    WatchDefaults.watchDelay = 10.milliseconds
    RocksDBDefaults.filesChangedDebounce = 15.milliseconds
}
