package org.archivekeep.cli.commands

import kotlin.test.Test
import kotlin.test.assertEquals


class StatusTest : CommandTestBase() {

    @Test
    fun `test fully indexed archive`() {
        createTestingArchive()

        val out1 = executeCmd(currentArchivePath, "status")
        assertEquals("Total indexed files in archive: 6\n", out1)

        val out2 = executeCmd(currentArchivePath.resolve("dir"), "status")
        assertEquals("Total indexed files in archive: 6\n", out2)
    }

    @Test
    fun `test partially indexed archive`() {
        createTestingArchive()

        createUnindexedFiles(
            mapOf(
                "c" to "file_c",
                "d" to "file_d",
                "e" to "file_e",
                "dir/c" to "dir file c",
                "dir/f" to "dir file f",
                "other/d" to "other file d",
                "other/g" to "other file g",
            )
        )

        data class TC(
            val subpath: List<String>,
            val args: List<String>,
            val out: String
        )

        listOf(
            TC(
                emptyList(),
                listOf("status"),
                "\nFiles not added to the archive:\n\tc\n\td\n\tdir/c\n\tdir/f\n\te\n\tother/d\n\tother/g\n\nTotal indexed files in archive: 6\n",
            ),
            TC(
                listOf("dir"),
                listOf("status"),
                "\nFiles not added to the archive:\n\t../c\n\t../d\n\tc\n\tf\n\t../e\n\t../other/d\n\t../other/g\n\nTotal indexed files in archive: 6\n",
            ),
            TC(
                listOf("other"),
                listOf("status"),
                "\nFiles not added to the archive:\n\t../c\n\t../d\n\t../dir/c\n\t../dir/f\n\t../e\n\td\n\tg\n\nTotal indexed files in archive: 6\n",
            ),
            TC(
                emptyList(),
                listOf("status", "dir"),
                "\nFiles not added to the archive:\n\tdir/c\n\tdir/f\n\nFiles indexed in archive matching globs: 2\n",
            ),
            TC(
                listOf("dir"),
                listOf("status", "."),
                "\nFiles not added to the archive:\n\tc\n\tf\n\nFiles indexed in archive matching globs: 2\n",
            ),
            TC(
                listOf("dir"),
                listOf("status", "../other"),
                "\nFiles not added to the archive:\n\t../other/d\n\t../other/g\n\nFiles indexed in archive matching globs: 2\n",
            ),
            TC(
                listOf("dir"),
                listOf("status", ".", "../other"),
                "\nFiles not added to the archive:\n\tc\n\tf\n\t../other/d\n\t../other/g\n\nFiles indexed in archive matching globs: 4\n",
            )
        ).forEach {tc ->
            val out = executeCmd(
                tc.subpath.fold(currentArchivePath) { acc, o -> acc.resolve(o)},
                *tc.args.toTypedArray()
            )
            assertEquals(tc.out, out)
        }
    }
}
