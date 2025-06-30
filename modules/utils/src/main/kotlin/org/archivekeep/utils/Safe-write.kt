package org.archivekeep.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.readText

suspend fun safeFileRead(path: Path): String? {
    // TODO: check for swap file

    return if (path.exists()) path.readText() else null
}

suspend fun safeFileReadWrite(
    path: Path,
    transform: suspend (existing: String?) -> String,
) {
    val swapPath = path.resolveSibling(path.fileName.toString() + ".swap")

    if (!createWait(swapPath.toFile())) {
        throw RuntimeException("File $path locked, found $swapPath")
    }

    try {
        val contents = if (path.exists()) path.readText() else null
        val newContents = transform(contents)

        swapPath.toFile().writeText(newContents)

        swapPath.moveTo(path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        swapPath.deleteIfExists()
    }
}

private suspend fun createWait(swapFile: File): Boolean {
    var createdNew = false

    for (i in 0..100) {
        createdNew = (
            withContext(Dispatchers.IO) {
                swapFile.createNewFile()
            }
        )

        if (createdNew) {
            break
        }

        delay(100)
    }

    return createdNew
}
