package org.archivekeep.files.driver.filesystem.files.objectbox

import io.objectbox.BoxStore
import java.nio.file.Path

actual fun createBoxStore(path: Path): BoxStore =
    MyObjectBox
        .builder()
        .directory(path.toFile())
        .build()
