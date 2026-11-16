package org.archivekeep.files

import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.utils.hashing.sha256

suspend fun (Repo).quickSave(
    name: String,
    contents: String = name,
) {
    val contentsBytes = contents.toByteArray()

    save(name, ArchiveFileInfo(contentsBytes.size.toLong(), checksumSha256 = contentsBytes.sha256()), contentsBytes.inputStream())
}

fun IndexUpdateProcedure.DiscoveryResult.NewFile.Companion.fromStringContents(
    path: String,
    contents: String = path,
): IndexUpdateProcedure.DiscoveryResult.NewFile = IndexUpdateProcedure.DiscoveryResult.NewFile(path, contents.toByteArray().size.toLong())
