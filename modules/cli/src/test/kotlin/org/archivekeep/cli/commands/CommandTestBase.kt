package org.archivekeep.cli.commands

import org.archivekeep.cli.MainCommand
import org.archivekeep.cli.utils.sha256
import org.archivekeep.files.repo.files.FilesRepo
import org.junit.jupiter.api.io.TempDir
import picocli.CommandLine
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.writeText
import kotlin.test.assertEquals

abstract class CommandTestBase {
    @field:TempDir
    lateinit var baseTempDir: File

    val currentArchiveTempDir: File
        get() = baseTempDir.resolve("current-archive")

    val currentArchivePath: Path
        get() = Path(currentArchiveTempDir.absolutePath)

    internal fun fileRepo() = FilesRepo(currentArchivePath)

    internal fun executeCmd(
        cwd: Path,
        vararg args: String,
        // TODO: interactive mode - listOf(ExpectOut("..."), ProvideIn("..."),...)
        `in`: String = "",
        expectedOut: String? = null,
    ): String {
        val app = MainCommand(cwd, EmptyCoroutineContext, inStream = `in`.byteInputStream())
        val cmd = CommandLine(app)

        val sw = StringWriter()
        cmd.setOut(PrintWriter(sw))

        val exitCode: Int = cmd.execute(*args)
        val out = sw.toString()

        assertEquals(0, exitCode)
        expectedOut?.let {
            assertEquals(expectedOut, out)
        }

        return out
    }

    internal fun executeFailingCmd(
        cwd: Path,
        vararg args: String,
        `in`: String = "",
    ): Pair<Int, String> {
        val app = MainCommand(cwd, EmptyCoroutineContext, inStream = `in`.byteInputStream())
        val cmd = CommandLine(app)

        val sw = StringWriter()
        cmd.setOut(PrintWriter(sw))

        val exitCode: Int = cmd.execute(*args)

        return Pair(exitCode, sw.toString())
    }

    fun createTestingArchive() {
        createCurrentArchiveWithContents(
            mapOf(
                "a" to "file_a",
                "b" to "file_b",
                "dir/a" to "dir file a",
                "dir/b" to "dir file b",
                "other/a" to "other file a",
                "other/c" to "other file c",
            ),
        )
    }

    fun createCurrentArchiveWithContents(files: Map<String, String>) {
        createUnindexedFiles(files)

        files.forEach {
            val checksumPath = currentArchiveTempDir.resolve(".archive/checksums").resolve(it.key + ".sha256").toPath()
            checksumPath.createParentDirectories()

            checksumPath.writeText(it.value.sha256())
        }
    }

    internal fun createUnindexedFiles(files: Map<String, String>) {
        files.forEach {
            val filePath = currentArchiveTempDir.resolve(it.key).toPath()
            filePath.createParentDirectories()

            filePath.writeText(it.value)
        }
    }

    internal fun createMissingFiles(files: Map<String, String>) {
        files.forEach {
            val checksumPath = currentArchiveTempDir.resolve(".archive/checksums").resolve(it.key + ".sha256").toPath()
            checksumPath.createParentDirectories()

            checksumPath.writeText(it.value.sha256())
        }
    }

    @OptIn(ExperimentalPathApi::class)
    internal fun cleanup() {
        // TODO: change to parametrized test, and don't do manual cleanup

        if (currentArchiveTempDir.exists()) {
            currentArchiveTempDir.toPath().forEachDirectoryEntry {
                println("to delete: $it")
                it.deleteRecursively()
            }
        }
    }

    internal fun terminalLines(vararg lines: String): String = lines.joinToString("\n") + "\n"
}
