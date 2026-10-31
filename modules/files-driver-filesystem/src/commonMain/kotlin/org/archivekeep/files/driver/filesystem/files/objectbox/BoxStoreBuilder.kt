package org.archivekeep.files.driver.filesystem.files.objectbox

import io.objectbox.BoxStore
import java.nio.file.Path

expect fun createBoxStore(path: Path): BoxStore
