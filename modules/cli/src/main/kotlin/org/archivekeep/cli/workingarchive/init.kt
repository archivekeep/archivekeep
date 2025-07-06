package org.archivekeep.cli.workingarchive

import org.archivekeep.files.repo.files.FilesRepo
import java.nio.file.Path

fun init(baseDirectory: Path) {
    FilesRepo.create(baseDirectory)
}
