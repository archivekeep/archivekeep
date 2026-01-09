package org.archivekeep.utils.io

import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.function.BiFunction
import kotlin.random.Random

/**
 * The createTmpFileForWrite function creates a new temporary file to write a new file into.
 *
 * In case of abrupt interrupt, don't leave unfinished copy under real filename.
 */
fun <T> createTmpFileForWrite(
    dstPath: Path,
    opener: BiFunction<Path, Array<OpenOption>, T>,
    openOptions: Array<OpenOption> = arrayOf(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
): Pair<Path, T> {
    var dstTmpFilePath: Path

    do {
        dstTmpFilePath = dstPath.parent.resolve("unfinished-copy-${Random.nextInt(1, 999_999)}.tmp")

        try {
            val fc = opener.apply(dstTmpFilePath, openOptions)

            return Pair(dstTmpFilePath, fc)
        } catch (e: FileAlreadyExistsException) {
            continue
        }
    } while (true)
}

fun moveTmpToDestination(
    dstTmpFilePath: Path,
    dstPath: Path,
) {
    Files.move(
        dstTmpFilePath,
        dstPath,
        StandardCopyOption.ATOMIC_MOVE,
    )

    FileChannel.open(
        dstPath.parent,
        StandardOpenOption.READ,
    ).use {
        // to be double-sure, sync parent
        it.force(true)
    }
}
