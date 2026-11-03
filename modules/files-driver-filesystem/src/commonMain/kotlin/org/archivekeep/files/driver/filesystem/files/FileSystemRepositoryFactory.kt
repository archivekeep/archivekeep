package org.archivekeep.files.driver.filesystem.files

import org.archivekeep.files.driver.filesystem.files.rocksdb.FilesRocksDbRepo
import java.nio.file.Path

object FileSystemRepositoryFactory {
    fun openPlainRepositoryOrNull(path: Path): FilesystemWorkingRepository? =
        FilesRocksDbRepo.openOrNull(path)
            ?: FilesSqliteRepo.openOrNull(path)
            ?: FilesRepo.openOrNull(path)

    suspend fun isPlainRepository(path: Path): Boolean = openPlainRepositoryOrNull(path)?.also { it.close() } != null

    suspend fun createPlainRepositoryAndClose(
        path: Path,
        type: PlainRepositoryType,
    ) {
        when (type) {
            PlainRepositoryType.ROCKS_DB -> FilesRocksDbRepo.create(path).also { it.close() }
            PlainRepositoryType.SQLITE -> FilesSqliteRepo.create(path).also { it.close() }
            PlainRepositoryType.CHECKSUM_FILES -> FilesRepo.create(path).also { it.close() }
        }
    }
}
