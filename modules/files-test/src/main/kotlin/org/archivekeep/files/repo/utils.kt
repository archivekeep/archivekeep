package org.archivekeep.files.repo

import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.utils.hashing.sha256
import kotlin.test.assertEquals

fun ArchiveFileInfo.Companion.forStringContents(contents: String): ArchiveFileInfo {
    val contentsBytes = contents.toByteArray()

    return ArchiveFileInfo(
        length = contents.toByteArray().size.toLong(),
        checksumSha256 = contentsBytes.sha256(),
    )
}

fun RepoIndex.File.Companion.forStringContents(
    path: String,
    stringContents: String = path,
): RepoIndex.File =
    RepoIndex.File(
        path = path,
        size = stringContents.length.toLong(),
        checksumSha256 = stringContents.sha256(),
    )

suspend fun (Repo).assertFileHasStringContents(
    filename: String,
    expectedContents: String = filename,
) {
    open(filename) { info, stream ->
        val contents = String(stream.readAllBytes())

        assertEquals(ArchiveFileInfo.forStringContents(expectedContents), info)
        assertEquals(contents, expectedContents)
    }
}
