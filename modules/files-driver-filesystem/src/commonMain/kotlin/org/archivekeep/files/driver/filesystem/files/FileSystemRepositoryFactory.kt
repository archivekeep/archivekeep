package org.archivekeep.files.driver.filesystem.files

import org.archivekeep.files.driver.filesystem.files.rocksdb.FilesRocksDbRepoFactory
import org.archivekeep.files.driver.filesystem.files.sqlite.FilesSqliteRepoFactory
import java.nio.file.Path

object FileSystemRepositoryFactory {
    suspend fun openPlainRepositoryOrNull(path: Path): FilesystemWorkingRepository? =
        FilesRocksDbRepoFactory.openOrNull(path)
            ?: FilesSqliteRepoFactory.openOrNull(path)
            ?: FilesRepo.openOrNull(path)

    suspend fun isPlainRepository(path: Path): Boolean = openPlainRepositoryOrNull(path)?.also { it.close() } != null

    suspend fun createPlainRepositoryAndClose(
        path: Path,
        type: PlainRepositoryType,
    ) {
        when (type) {
            PlainRepositoryType.ROCKS_DB -> FilesRocksDbRepoFactory.create(path).also { it.close() }
            PlainRepositoryType.SQLITE -> FilesSqliteRepoFactory.create(path).also { it.close() }
            PlainRepositoryType.CHECKSUM_FILES -> FilesRepo.create(path).also { it.close() }
        }
    }
}
