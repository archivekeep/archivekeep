package org.archivekeep.cli.commands

import org.archivekeep.createArchiveWithContents
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals


class CompareTest : CommandTestBase() {

    @Test
    fun execute(@TempDir otherDir: File) {
        createCurrentArchiveWithContents(
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

        val out1 = executeCmd(currentArchivePath, "compare", otherDir.path)
        assertEquals("""
            
            Extra files in local archive:
            	file to be extra in source
            	file to modify with backup
            	file to overwrite

            Extra files in remote archive:
            	file to be extra in target
            	file to overwrite

            Files to be moved in remote to match local:
            	{} -> file to duplicate 02
            	file to move and duplicate -> {file to move and duplicate/01, file to move and duplicate/02}
            	[31m{ -> moved/}[0mfile to move
            	[31m{ -> old/}[0m[31m{file to modify with backup -> file to modify backup}[0m

            Extra files in local archive: 3
            Extra files in remote archive: 2
            Total files present in both archives: 7
            
        """.trimIndent() + "\n", out1)
    }
}
