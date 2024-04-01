package org.archivekeep.core.operations

import org.archivekeep.core.repo.files.FilesRepo
import org.archivekeep.createArchiveWithContents
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CompareOperationTest {

    @Test
    fun execute(@TempDir baseDir: File, @TempDir otherDir: File) {
        createArchiveWithContents(
            baseDir, mapOf(
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
            )
        )
        createArchiveWithContents(
            otherDir, mapOf(
                "file to be extra in target" to "file to be extra in target",
                "file to duplicate" to "file to duplicate: old",
                "file to move" to "file to move: old",
                "file to move and duplicate" to "file to move and duplicate: old",
                "file to modify with backup" to "file to modify with backup: old",
                "file to overwrite" to "file to overwrite: old",
                "file to be left untouched" to "file to be left untouched: untouched",
            )
        )

        val result = CompareOperation().execute(
            FilesRepo(baseDir.toPath()),
            FilesRepo(otherDir.toPath()),
        )

        assertIterableEquals(
            listOf(
                CompareOperation.Result.Relocation(
                    checksum = "12cd1a39a3b65fe7e821ba6467f7339563e32329a7e2d8f0386bc55f3d76f7db",
                    baseFilenames = listOf("file to duplicate", "file to duplicate 02"),
                    otherFilenames = listOf("file to duplicate"),
                ),
                CompareOperation.Result.Relocation(
                    checksum = "7d9d6af4e86c856a3cdedf6bfb72706e8e448e642e9e4d3de43849b83cc55faa",
                    baseFilenames = listOf("file to move and duplicate/01", "file to move and duplicate/02"),
                    otherFilenames = listOf("file to move and duplicate"),
                ),
                CompareOperation.Result.Relocation(
                    checksum = "e351823907f1408235e49f5f6df5a53a15a19c24c7c7f5eaf142215437da0504",
                    baseFilenames = listOf("moved/file to move"),
                    otherFilenames = listOf("file to move"),
                ),
                CompareOperation.Result.Relocation(
                    checksum = "4d740e8de8d39840f7ef07f6687001d50fe0a3960bdc8d079fd2ca4892c5f5b6",
                    baseFilenames = listOf("old/file to modify backup"),
                    otherFilenames = listOf("file to modify with backup"),
                ),
            ),
            result.relocations
        )

        assertIterableEquals(
            listOf(
                "file to modify with backup",
            ),
            result.newContentAfterMove
        )

        assertIterableEquals(
            listOf(
                "file to overwrite",
            ),
            result.newContentToOverwrite
        )

        assertIterableEquals(
            listOf(
                CompareOperation.Result.ExtraGroup(
                    checksum = "1edc8804c33fd71860f80dd0f5974f09c2cf9be162ae1dc8db0bfcb820e48cef",
                    filenames = listOf(
                        "file to be extra in source",
                    ),
                ),
                CompareOperation.Result.ExtraGroup(
                    checksum = "64ec974de61170bc2ba1041cde9a3a9fbe23ffbc4e5bdd8db8d149c2700c0b87",
                    filenames = listOf(
                        "file to modify with backup",
                    ),
                ),
                CompareOperation.Result.ExtraGroup(
                    checksum = "43cd4f8a83b9a4c06ffa4c62bf4d58dc3f48633faf4c2a15cb5ee897e54b6fb6",
                    filenames = listOf(
                        "file to overwrite",
                    ),
                )
            ),
            result.unmatchedBaseExtras
        )
        assertIterableEquals(
            listOf(
                CompareOperation.Result.ExtraGroup(
                    checksum = "c9dd58abb6a3bd6bfaf0b42d8e47b215002fbed488b53b165edfac151ff46d27",
                    filenames = listOf(
                        "file to be extra in target",
                    ),
                ),
                CompareOperation.Result.ExtraGroup(
                    checksum = "3e0db7aee818f73f48744520499f2e258351c9d8cef6da077cb722aff1fa8458",
                    filenames = listOf(
                        "file to overwrite",
                    ),
                ),
            ),
            result.unmatchedOtherExtras
        )
    }
}
