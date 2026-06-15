package org.archivekeep.files.driver.filesystem.files

import org.archivekeep.files.api.repository.LocalRepo

interface FilesystemWorkingRepository : LocalRepo {
    suspend fun deinitialize()
}
