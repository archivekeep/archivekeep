package org.archivekeep.files.driver.filesystem.files

import org.archivekeep.files.driver.filesystem.files.objectbox.FilesObjectBoxRepo
import java.nio.file.Path

object FileSystemRepositoryFactory {
    fun openPlainRepositoryOrNull(path: Path): FilesystemWorkingRepository? =
        FilesObjectBoxRepo.openOrNull(path)
            ?: FilesSqliteRepo.openOrNull(path)
            ?: FilesRepo.openOrNull(path)

    fun isPlainRepository(path: Path): Boolean = openPlainRepositoryOrNull(path) != null

    suspend fun createPlainRepositoryAndClose(
        path: Path,
        type: PlainRepositoryType,
    ) {
        when (type) {
            PlainRepositoryType.OBJECTBOX -> FilesObjectBoxRepo.create(path).also { it.close() }
            PlainRepositoryType.SQLITE -> FilesSqliteRepo.create(path)
            PlainRepositoryType.CHECKSUM_FILES -> FilesRepo.create(path)
        }
    }
}
