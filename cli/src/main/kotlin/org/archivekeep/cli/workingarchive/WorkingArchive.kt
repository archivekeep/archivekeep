package org.archivekeep.cli.workingarchive

import org.archivekeep.core.repo.Repo
import org.archivekeep.core.repo.files.openFilesRepoOrNull
import java.nio.file.Path
import kotlin.io.path.relativeTo

class WorkingArchive(
    val cwd: Path,
    val location: Path,
    val workingSubDirectory: Path,
    val relativePathToRoot: Path,
    val repo: Repo,
) {
    fun fromArchiveToRelativePath(path: String): Path = Path.of(path).relativeTo(workingSubDirectory)
}

fun openWorkingArchive(cwd: Path): WorkingArchive {
    var tryPath: Path? = cwd

    while (tryPath != null) {
        val archive = tryOpen(cwd, tryPath)

        if (archive != null) {
            return archive
        }

        tryPath = tryPath.parent
    }

    throw RuntimeException("Current working directory `$cwd` is not an archive")
}

private fun tryOpen(
    cwd: Path,
    tryPath: Path,
): WorkingArchive? {
    val filesRepo = openFilesRepoOrNull(tryPath)

    if (filesRepo != null) {
        return WorkingArchive(
            cwd = cwd,
            location = tryPath,
            workingSubDirectory = cwd.relativeTo(tryPath),
            relativePathToRoot = tryPath.relativeTo(cwd),
            repo = filesRepo,
        )
    }

    return null
}
