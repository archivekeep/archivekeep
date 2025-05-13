package org.archivekeep.cli.commands

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AddTest : CommandTestBase() {
    @Test
    fun `test partially indexed archive`() =
        runTest {
            data class TC(
                val subpath: List<String>,
                val args: List<String>,
                val out: String,
                val resultingIndex: List<String>,
            )

            listOf(
                TC(
                    emptyList(),
                    listOf("add", "dir"),
                    terminalLines(
                        "New files to be indexed:",
                        "\tdir/c",
                        "\tdir/f",
                        "",
                        "added: dir/c",
                        "added: dir/f",
                        "finished adding files",
                    ),
                    listOf(
                        "a",
                        "b",
                        "dir/a",
                        "dir/b",
                        "dir/c",
                        "dir/f",
                        "other/a",
                        "other/c",
                    ),
                ),
                TC(
                    listOf("dir"),
                    listOf("add", "."),
                    terminalLines(
                        "New files to be indexed:",
                        "\tc",
                        "\tf",
                        "",
                        "added: c",
                        "added: f",
                        "finished adding files",
                    ),
                    listOf(
                        "a",
                        "b",
                        "dir/a",
                        "dir/b",
                        "dir/c",
                        "dir/f",
                        "other/a",
                        "other/c",
                    ),
                ),
                TC(
                    listOf("dir"),
                    listOf("add", "../other"),
                    terminalLines(
                        "New files to be indexed:",
                        "\t../other/d",
                        "\t../other/g",
                        "",
                        "added: ../other/d",
                        "added: ../other/g",
                        "finished adding files",
                    ),
                    listOf(
                        "a",
                        "b",
                        "dir/a",
                        "dir/b",
                        "other/a",
                        "other/c",
                        "other/d",
                        "other/g",
                    ),
                ),
                TC(
                    listOf("dir"),
                    listOf("add", "../dir/f", "../e", "../other/g"),
                    terminalLines(
                        "New files to be indexed:",
                        "\tf",
                        "\t../e",
                        "\t../other/g",
                        "",
                        "added: f",
                        "added: ../e",
                        "added: ../other/g",
                        "finished adding files",
                    ),
                    listOf(
                        "a",
                        "b",
                        "dir/a",
                        "dir/b",
                        "dir/f",
                        "e",
                        "other/a",
                        "other/c",
                        "other/g",
                    ),
                ),
            ).forEach { tc ->
                cleanup()
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
                    ),
                )

                val out =
                    executeCmd(
                        tc.subpath.fold(currentArchivePath) { acc, o -> acc.resolve(o) },
                        *tc.args.toTypedArray(),
                    )
                assertEquals(tc.out, out)
                assertEquals(tc.resultingIndex, fileRepo().indexedFilenames())
            }
        }

    @Test
    fun `test partially indexed archive with missing files`() =
        runTest {
            data class TC(
                val subpath: List<String>,
                val args: List<String>,
                val `in`: String,
                val out: String,
                val resultingIndex: List<String>,
            )

            listOf(
                TC(
                    listOf("dir"),
                    listOf("add", "."),
                    "y\n",
                    terminalLines(
                        "Missing indexed files not matched by add:",
                        "\t../missing/unexisting_x",
                        "",
                        "New files to be indexed:",
                        "\tf",
                        "",
                        "Files to be moved:",
                        "\t../missing/old_c -> c",
                        "",
                        "",
                        "Do want to perform move? [y/n]: ",
                        "proceeding ...",
                        "moved: ../missing/old_c to c",
                        "finished moving files",
                        "added: f",
                        "finished adding files",
                    ),
                    listOf(
                        "a",
                        "b",
                        "dir/a",
                        "dir/b",
                        "dir/c",
                        "dir/f",
                        "missing/unexisting_x",
                        "other/a",
                        "other/c",
                    ),
                ),
            ).forEach { tc ->
                cleanup()
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
                    ),
                )

                createMissingFiles(
                    mapOf(
                        "missing/old_c" to "dir file c",
                        "missing/unexisting_x" to "file x",
                    ),
                )

                val out =
                    executeCmd(
                        tc.subpath.fold(currentArchivePath) { acc, o -> acc.resolve(o) },
                        *tc.args.toTypedArray(),
                        `in` = tc.`in`,
                    )
                assertEquals(tc.out, out)
                assertEquals(tc.resultingIndex, fileRepo().indexedFilenames())
            }
        }
}
