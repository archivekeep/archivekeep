package org.archivekeep.files.operations

import kotlinx.coroutines.runBlocking
import org.archivekeep.files.api.repository.operations.CompareOperation
import org.archivekeep.files.createArchiveWithContents
import org.archivekeep.files.driver.filesystem.files.FilesRepo
import org.archivekeep.utils.hashing.sha256
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CompareOperationTest {
    @Test
    fun execute(
        @TempDir t: File,
    ) = runBlocking {
        val baseDir =
            t
                .toPath()
                .resolve("base")
                .toFile()
                .apply { mkdirs() }
        val otherDir =
            t
                .toPath()
                .resolve("other")
                .toFile()
                .apply { mkdirs() }

        createArchiveWithContents(
            baseDir,
            mapOf(
                "file to be extra in source" to "file to be extra in source",
                "file to duplicate" to "file to duplicate: old",
                "file to duplicate 02" to "file to duplicate: old",
                "file to modify with backup" to "file to modify with backup: new",
                "file to move and duplicate/01" to "file to move and duplicate: old",
                "file to move and duplicate/02" to "file to move and duplicate: old",
                "file to overwrite" to "file to overwrite: new",
                "file to be left untouched" to "file to be left untouched: untouched",
                "moved/file to move" to "file to move: old",
                "old/file to modify backup" to "file to modify with backup: old",
            ),
        )
        createArchiveWithContents(
            otherDir,
            mapOf(
                "file to be extra in target" to "file to be extra in target",
                "file to duplicate" to "file to duplicate: old",
                "file to move" to "file to move: old",
                "file to move and duplicate" to "file to move and duplicate: old",
                "file to modify with backup" to "file to modify with backup: old",
                "file to overwrite" to "file to overwrite: old",
                "file to be left untouched" to "file to be left untouched: untouched",
            ),
        )

        val result =
            CompareOperation().execute(
                FilesRepo(baseDir.toPath()),
                FilesRepo(otherDir.toPath()),
            )

        assertIterableEquals(
            listOf(
                CompareOperation.Result.Relocation.forStringContents(
                    "file to duplicate: old",
                    baseFilenames = listOf("file to duplicate", "file to duplicate 02"),
                    otherFilenames = listOf("file to duplicate"),
                ),
                CompareOperation.Result.Relocation.forStringContents(
                    "file to move and duplicate: old",
                    baseFilenames = listOf("file to move and duplicate/01", "file to move and duplicate/02"),
                    otherFilenames = listOf("file to move and duplicate"),
                ),
                CompareOperation.Result.Relocation.forStringContents(
                    "file to move: old",
                    baseFilenames = listOf("moved/file to move"),
                    otherFilenames = listOf("file to move"),
                ),
                CompareOperation.Result.Relocation.forStringContents(
                    "file to modify with backup: old",
                    baseFilenames = listOf("old/file to modify backup"),
                    otherFilenames = listOf("file to modify with backup"),
                ),
            ),
            result.relocations,
        )

        assertIterableEquals(
            listOf(
                "file to modify with backup",
            ),
            result.newContentAfterMove,
        )

        assertIterableEquals(
            listOf(
                "file to overwrite",
            ),
            result.newContentToOverwrite,
        )

        assertIterableEquals(
            listOf(
                CompareOperation.Result.ExtraGroup.forStringContents(
                    "file to be extra in source",
                    filenames =
                        listOf(
                            "file to be extra in source",
                        ),
                ),
                CompareOperation.Result.ExtraGroup.forStringContents(
                    "file to modify with backup: new",
                    filenames =
                        listOf(
                            "file to modify with backup",
                        ),
                ),
                CompareOperation.Result.ExtraGroup.forStringContents(
                    "file to overwrite: new",
                    filenames =
                        listOf(
                            "file to overwrite",
                        ),
                ),
            ),
            result.unmatchedBaseExtras,
        )
        assertIterableEquals(
            listOf(
                CompareOperation.Result.ExtraGroup.forStringContents(
                    "file to be extra in target",
                    filenames =
                        listOf(
                            "file to be extra in target",
                        ),
                ),
                CompareOperation.Result.ExtraGroup.forStringContents(
                    "file to overwrite: old",
                    filenames =
                        listOf(
                            "file to overwrite",
                        ),
                ),
            ),
            result.unmatchedOtherExtras,
        )
    }
}

private fun CompareOperation.Result.Relocation.Companion.forStringContents(
    stringContents: String,
    baseFilenames: List<String>,
    otherFilenames: List<String>,
): CompareOperation.Result.Relocation =
    CompareOperation.Result.Relocation(
        checksum = stringContents.sha256(),
        fileSize = stringContents.length.toLong(),
        baseFilenames = baseFilenames,
        otherFilenames = otherFilenames,
    )

private fun CompareOperation.Result.ExtraGroup.Companion.forStringContents(
    stringContents: String,
    filenames: List<String>,
): CompareOperation.Result.ExtraGroup =
    CompareOperation.Result.ExtraGroup(
        checksum = stringContents.sha256(),
        fileSize = stringContents.length.toLong(),
        filenames = filenames,
    )
