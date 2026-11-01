package org.archivekeep.cli.workingarchive

import org.archivekeep.files.driver.filesystem.files.FileSystemRepositoryFactory
import org.archivekeep.files.driver.filesystem.files.PlainRepositoryType
import java.nio.file.Path

suspend fun init(baseDirectory: Path) {
    // TODO: add support for other types
    FileSystemRepositoryFactory.createPlainRepositoryAndClose(baseDirectory, PlainRepositoryType.CHECKSUM_FILES)
}
