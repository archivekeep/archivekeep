package org.archivekeep.files.driver.filesystem.files

import org.archivekeep.files.api.repository.LocalRepo
import java.nio.file.Path

interface FilesystemWorkingRepository : LocalRepo {
    val root: Path

    suspend fun deinitialize()
}
