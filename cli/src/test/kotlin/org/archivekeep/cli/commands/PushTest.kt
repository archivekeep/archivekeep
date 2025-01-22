package org.archivekeep.cli.commands

import org.archivekeep.createArchiveWithContents
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class PushTest : CommandTestBase() {
    @Test
    fun `pushes new files`(
        @TempDir dstArchiveDir: File,
    ) {
        createCurrentArchiveWithContents(
            mapOf(
                "a" to "A",
                "b" to "B",
                "c" to "C",
                "d" to "D",
            ),
        )

        createArchiveWithContents(
            dstArchiveDir,
            mapOf(
                "a" to "A",
            ),
        )

        executeCmd(
            currentArchivePath,
            args =
                arrayOf(
                    "push",
                    dstArchiveDir.path,
                ),
            `in` = "yes\n",
            expectedOut =
                """
                
                Extra files in current archive:
                	b
                	c
                	d
                
                Extra files in current archive: 3
                Extra files in other archive: 0
                Total files present in both archives: 1

                Do you want to push new files? [y/n]: 
                file stored: b
                file stored: c
                file stored: d
                """.trimIndent() + "\n",
        )
    }

    @Test
    fun `moves requires flag`(
        @TempDir dstArchiveDir: File,
    ) {
        createCurrentArchiveWithContents(
            mapOf(
                "a" to "A",
            ),
        )

        createArchiveWithContents(
            dstArchiveDir,
            mapOf(
                "old/a" to "A",
            ),
        )

        run {
            val (exitCode, out) =
                executeFailingCmd(
                    currentArchivePath,
                    args =
                        arrayOf(
                            "push",
                            dstArchiveDir.path,
                        ),
                )

            assertEquals(1, exitCode)
            assertEquals(
                """
                
                Files to be moved in other to match current:
                	[31m{old/ -> }[0ma
                
                Extra files in current archive: 0
                Extra files in other archive: 0
                Total files present in both archives: 1

                relocations disabled but present
                Enable relocations with --resolve-moves, or switch to --additive-duplicating mode
                """.trimIndent() + "\n",
                out,
            )
        }

        executeCmd(
            currentArchivePath,
            args =
                arrayOf(
                    "push",
                    dstArchiveDir.path,
                    "--resolve-moves",
                ),
            `in` = "yes\n",
            expectedOut =
                """
                
                Files to be moved in other to match current:
                	[31m{old/ -> }[0ma

                Extra files in current archive: 0
                Extra files in other archive: 0
                Total files present in both archives: 1
                
                Do you want to push moves? [y/n]: 
                file moved: old/a -> a
                """.trimIndent() + "\n",
        )
    }

    @Test
    fun `pushes new duplications requires flag(s)`(
        @TempDir dstArchiveDir: File,
    ) {
        createDuplicationsRepoPair(dstArchiveDir)

        run {
            val (exitCode, out) =
                executeFailingCmd(
                    currentArchivePath,
                    args =
                        arrayOf(
                            "push",
                            dstArchiveDir.path,
                        ),
                )

            assertEquals(1, exitCode)
            assertEquals(
                """
                
                Files to be moved in other to match current:
                	{} -> duplication/a

                Extra files in current archive: 0
                Extra files in other archive: 0
                Total files present in both archives: 2
                
                relocations disabled but present
                Enable relocations with --resolve-moves, or switch to --additive-duplicating mode
                """.trimIndent() + "\n",
                out,
            )

            // TODO: assert resulting archive repository contents
        }

        run {
            val (exitCode, out) =
                executeFailingCmd(
                    currentArchivePath,
                    args =
                        arrayOf(
                            "push",
                            "--resolve-moves",
                            dstArchiveDir.path,
                        ),
                )

            assertEquals(1, exitCode)
            assertEquals(
                """
                
                Files to be moved in other to match current:
                	{} -> duplication/a

                Extra files in current archive: 0
                Extra files in other archive: 0
                Total files present in both archives: 2

                duplicate increase is not allowed
                Enable duplication increase with --allow-duplicate-increase, or switch to --additive-duplicating mode
                """.trimIndent() + "\n",
                out,
            )

            // TODO: assert resulting archive repository contents
        }
    }

    @Test
    fun `pushes new duplications with --allow-duplicate-increase`(
        @TempDir dstArchiveDir: File,
    ) {
        createDuplicationsRepoPair(dstArchiveDir)

        executeCmd(
            currentArchivePath,
            args =
                arrayOf(
                    "push",
                    "--resolve-moves",
                    "--allow-duplicate-increase",
                    dstArchiveDir.path,
                ),
            `in` = "yes\n",
            expectedOut =
                """
                
                Files to be moved in other to match current:
                	{} -> duplication/a

                Extra files in current archive: 0
                Extra files in other archive: 0
                Total files present in both archives: 2

                Do you want to push moves? [y/n]: 
                file stored: duplication/a
                """.trimIndent() + "\n",
        )

        // TODO: assert resulting archive repository contents
    }

    @Test
    fun `pushes new duplications with --additive-duplicating mode`(
        @TempDir dstArchiveDir: File,
    ) {
        createDuplicationsRepoPair(dstArchiveDir)

        executeCmd(
            currentArchivePath,
            args =
                arrayOf(
                    "push",
                    "--resolve-moves",
                    "--additive-duplicating",
                    dstArchiveDir.path,
                ),
            `in` = "yes\n",
            expectedOut =
                """
                
                Files to be moved in other to match current:
                	{} -> duplication/a

                Extra files in current archive: 0
                Extra files in other archive: 0
                Total files present in both archives: 2

                Do you want to push in additive duplicating mode? [y/n]: 
                file stored: duplication/a
                """.trimIndent() + "\n",
        )

        // TODO: assert resulting archive repository contents
    }

    @Test
    fun `resolves moves and relocations with --allow-duplicate-increase`(
        @TempDir dstArchiveDir: File,
    ) {
        createDuplicationsAndRelocationRepoPair(dstArchiveDir)

        executeCmd(
            currentArchivePath,
            args =
                arrayOf(
                    "push",
                    "--resolve-moves",
                    "--allow-duplicate-increase",
                    dstArchiveDir.path,
                ),
            `in` = "yes\nyes\n",
            expectedOut =
                """
                
                Extra files in current archive:
                	new/f

                Files to be moved in other to match current:
                	{} -> duplicated/e
                	old/b -> {relocated and duplicated/b, relocated and duplicated/c}
                	[31m{ol -> relocate}[0md/a

                Extra files in current archive: 1
                Extra files in other archive: 0
                Total files present in both archives: 5

                Do you want to push moves? [y/n]: 
                file stored: duplicated/e
                file stored: relocated and duplicated/c
                file moved: old/b -> relocated and duplicated/b
                file moved: old/a -> relocated/a
                Do you want to push new files? [y/n]: 
                file stored: new/f
                """.trimIndent() + "\n",
        )

        // TODO: assert resulting archive repository contents
    }

    @Test
    fun `adds moves and relocations as duplications with --additive-duplicating mode`(
        @TempDir dstArchiveDir: File,
    ) {
        createDuplicationsAndRelocationRepoPair(dstArchiveDir)

        executeCmd(
            currentArchivePath,
            args =
                arrayOf(
                    "push",
                    "--resolve-moves",
                    "--additive-duplicating",
                    dstArchiveDir.path,
                ),
            `in` = "yes\nyes\n",
            expectedOut =
                """
                
                Extra files in current archive:
                	new/f
                
                Files to be moved in other to match current:
                	{} -> duplicated/e
                	old/b -> {relocated and duplicated/b, relocated and duplicated/c}
                	[31m{ol -> relocate}[0md/a
                
                Extra files in current archive: 1
                Extra files in other archive: 0
                Total files present in both archives: 5
                
                Do you want to push in additive duplicating mode? [y/n]: 
                file stored: duplicated/e
                file stored: relocated and duplicated/b
                file stored: relocated and duplicated/c
                file stored: relocated/a
                Do you want to push new files? [y/n]: 
                file stored: new/f
                """.trimIndent() + "\n",
        )

        // TODO: assert resulting archive repository contents
    }

    private fun createDuplicationsRepoPair(dstArchiveDir: File) {
        createCurrentArchiveWithContents(
            mapOf(
                "a" to "A",
                "duplication/a" to "A",
            ),
        )

        createArchiveWithContents(
            dstArchiveDir,
            mapOf(
                "a" to "A",
            ),
        )
    }

    private fun createDuplicationsAndRelocationRepoPair(dstArchiveDir: File) {
        createCurrentArchiveWithContents(
            mapOf(
                "relocated/a" to "relocated",
                "relocated and duplicated/b" to "relocated and duplicated",
                "relocated and duplicated/c" to "relocated and duplicated",
                "duplicated/d" to "duplicated",
                "duplicated/e" to "duplicated",
                "new/f" to "new file",
            ),
        )

        createArchiveWithContents(
            dstArchiveDir,
            mapOf(
                "old/a" to "relocated",
                "old/b" to "relocated and duplicated",
                "duplicated/d" to "duplicated",
            ),
        )
    }
}
