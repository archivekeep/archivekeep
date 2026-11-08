package org.archivekeep.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.archivekeep.utils.hashing.sha256
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.readText

suspend fun safeFileRead(path: Path): String? = if (path.exists()) path.readText() else null

private fun syncToFile(file: File) {
    RandomAccessFile(file, "rw").use { it.channel.force(true) }
}

suspend fun safeFileReadWrite(
    path: Path,
    transform: suspend (existing: String?) -> String,
) {
    val swapPath = path.resolveSibling(path.fileName.toString() + ".swap")

    if (!createSwapFileWithRetry(swapPath.toFile())) {
        throw RuntimeException("File $path locked, found $swapPath")
    }

    try {
        val contents = if (path.exists()) path.readText() else null
        val newContents = transform(contents)

        val file = swapPath.toFile()
        file.writeText(newContents)
        syncToFile(file)

        swapPath.moveTo(path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        swapPath.deleteIfExists()
    }
}

private const val MaxBackupCount = 3

private fun Path.checksumPath() = resolveSibling("$fileName.sha256")

private fun Path.backupPath(index: Int) = resolveSibling("$fileName.bak$index")

suspend fun safeFileReadWriteWithChecksum(
    path: Path,
    content: String,
) {
    val swapPath = path.resolveSibling("${path.fileName}.swap")

    if (!createSwapFileWithRetry(swapPath.toFile())) {
        throw RuntimeException("File $path locked, found $swapPath")
    }

    try {
        rotateBackups(path)
        val file = swapPath.toFile()
        file.writeText(content)
        syncToFile(file)
        swapPath.moveTo(path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        writeChecksumAtomically(path, content.sha256())
    } finally {
        swapPath.deleteIfExists()
    }
}

private fun writeChecksumAtomically(
    path: Path,
    checksum: String,
) {
    val checksumFile = path.checksumPath()
    val checksumSwap = checksumFile.resolveSibling("${checksumFile.fileName}.swap")
    val file = checksumSwap.toFile()
    file.writeText(checksum)
    syncToFile(file)
    checksumSwap.moveTo(checksumFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
}

suspend fun safeFileReadWithChecksum(
    path: Path,
    fallback: () -> String?,
): String? {
    if (!path.exists()) {
        return null
    }

    val content = path.readText()
    val checksumFile = path.checksumPath()

    if (!checksumFile.exists()) {
        val expectedChecksum = content.sha256()
        try {
            val file = checksumFile.toFile()
            file.writeText(expectedChecksum)
            syncToFile(file)
            return content
        } catch (_: Throwable) {
            return content
        }
    }

    val storedChecksum = checksumFile.readText().trim()
    val actualChecksum = content.sha256()

    if (actualChecksum == storedChecksum) {
        return content
    }

    println("WARNING: Checksum mismatch for $path, attempting backup restore")
    val restored = restoreFromBackup(path)

    if (restored != null) {
        try {
            safeFileReadWriteWithChecksum(path, restored)
            println("Restored $path from backup successfully")
            return restored
        } catch (e: Throwable) {
            println("WARNING: Failed to write restored backup for $path: ${e.message}")
            return restored
        }
    }

    println("WARNING: No backup available for $path, re-syncing checksum")
    try {
        writeChecksumAtomically(path, actualChecksum)
        return content
    } catch (e: Throwable) {
        println("WARNING: Failed to write checksum for $path: ${e.message}")
        return fallback()
    }
}

private fun rotateBackups(path: Path) {
    if (!path.exists()) return

    val oldestBackup = path.backupPath(MaxBackupCount)
    if (oldestBackup.exists()) {
        oldestBackup.toFile().delete()
    }

    for (i in MaxBackupCount - 1 downTo 1) {
        val from = path.backupPath(i)
        val to = path.backupPath(i + 1)
        if (from.exists()) {
            from.moveTo(to, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    val bak1 = path.backupPath(1).toFile()
    path.toFile().copyTo(bak1, overwrite = true)
    syncToFile(bak1)
}

private fun restoreFromBackup(path: Path): String? {
    for (i in MaxBackupCount downTo 1) {
        val backup = path.backupPath(i)
        if (backup.exists()) {
            val content = backup.readText()
            if (content.isNotBlank()) {
                println("Restoring $path from ${backup.fileName}")
                return content
            }
        }
    }
    return null
}

fun cleanupChecksumAndBackups(path: Path) {
    path.checksumPath().toFile().delete()
    for (i in 1..MaxBackupCount) {
        path.backupPath(i).toFile().delete()
    }
}

private suspend fun createSwapFileWithRetry(swapFile: File): Boolean {
    for (i in 0..100) {
        if (withContext(Dispatchers.IO) { swapFile.createNewFile() }) {
            return true
        }
        delay(100)
    }
    return false
}
