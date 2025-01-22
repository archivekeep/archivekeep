package org.archivekeep.cli.workingarchive

import org.archivekeep.core.repo.files.createFilesRepo
import java.nio.file.Path

fun init(baseDirectory: Path) {
    createFilesRepo(baseDirectory)
}
