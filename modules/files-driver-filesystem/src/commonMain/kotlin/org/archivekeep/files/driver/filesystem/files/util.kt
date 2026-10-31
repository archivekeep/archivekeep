package org.archivekeep.files.driver.filesystem.files

import org.archivekeep.files.api.exceptions.MaliciousPath
import org.archivekeep.files.api.exceptions.NotNormalizedPath
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.invariantSeparatorsPathString

internal fun safeSubPath(path: String): Path = safeSubPath(Paths.get(path))

internal fun safeSubPath(path: Path): Path {
    val p = path.normalize()
    val s = p.invariantSeparatorsPathString

    if (s == ".." || s.startsWith("../")) {
        throw MaliciousPath(path.toString())
    }

    if (p.invariantSeparatorsPathString != path.toString()) {
        throw NotNormalizedPath(path.toString())
    }

    return p
}

/**
 * Compares two millisecond timestamps with second-granularity precision.
 *
 * On FAT/exFAT USB flash drives, Android's VFS layer adds sub-second noise
 * (e.g., 940ms) when reading timestamps, while Linux reads them cleanly.
 * Both platforms agree on the seconds part, so comparing at second granularity
 * eliminates the cross-platform noise.
 */
internal fun areTimestampsDifferent(
    fileMtimeMillis: Long,
    indexedMtimeMillis: Long,
): Boolean = fileMtimeMillis / 1000 != indexedMtimeMillis / 1000

internal fun computeChecksum(path: Path): String {
    val md = MessageDigest.getInstance("SHA-256")

    path.toFile().forEachBlock(32 * 1024) { buffer, bytesRead ->
        md.update(buffer, 0, bytesRead)
    }

    val digest = md.digest()

    return digest.joinToString("") { "%02x".format(it) }
}
