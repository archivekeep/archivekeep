package org.archivekeep.files

import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.Repo
import org.archivekeep.utils.sha256

suspend fun (Repo).quickSave(
    name: String,
    contents: String = name,
) {
    val contentsBytes = contents.toByteArray()

    save(name, ArchiveFileInfo(contentsBytes.size.toLong(), checksumSha256 = contentsBytes.sha256()), contentsBytes.inputStream())
}

fun IndexUpdateProcedure.PreparationResult.NewFile.Companion.fromStringContents(
    path: String,
    contents: String = path,
): IndexUpdateProcedure.PreparationResult.NewFile = IndexUpdateProcedure.PreparationResult.NewFile(path, contents.toByteArray().size.toLong())
