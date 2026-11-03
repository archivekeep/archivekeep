package org.archivekeep.files.driver.filesystem.files

import java.nio.file.Path

object FileSystemRepositoryFactory {
    fun openPlainRepositoryOrNull(path: Path): FilesystemWorkingRepository? =
        FilesSqliteRepo.openOrNull(path)
            ?: FilesRepo.openOrNull(path)

    suspend fun isPlainRepository(path: Path): Boolean = openPlainRepositoryOrNull(path)?.also { it.close() } != null

    suspend fun createPlainRepositoryAndClose(
        path: Path,
        type: PlainRepositoryType,
    ) {
        when (type) {
            PlainRepositoryType.SQLITE -> FilesSqliteRepo.create(path).also { it.close() }
            PlainRepositoryType.CHECKSUM_FILES -> FilesRepo.create(path).also { it.close() }
        }
    }
}
