package org.archivekeep.cli.workingarchive

import org.archivekeep.files.driver.filesystem.files.FilesRepo
import java.nio.file.Path

suspend fun init(baseDirectory: Path) {
    FilesRepo.create(baseDirectory)
}
