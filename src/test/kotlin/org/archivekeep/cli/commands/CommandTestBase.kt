package org.archivekeep.cli.commands

import org.archivekeep.cli.MainCommand
import org.junit.jupiter.api.io.TempDir
import picocli.CommandLine
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals

abstract class CommandTestBase {

    @field:TempDir
    lateinit var archiveTempDir: File

    val archivePath: Path
        get() = Path(archiveTempDir.absolutePath)

    internal fun executeCmd(cwd: Path, vararg args: String): String {
        val app = MainCommand(cwd)
        val cmd = CommandLine(app)

        val sw = StringWriter()
        cmd.setOut(PrintWriter(sw))

        val exitCode: Int = cmd.execute(*args)
        assertEquals(0, exitCode)

        return sw.toString()
    }

    fun createTestingArchive() {
        createArchiveWithContents(
            mapOf(
                "a" to "file_a",
                "b" to "file_b",
                "dir/a" to "dir file a",
                "dir/b" to "dir file b",
                "other/a" to "other file a",
                "other/c" to "other file c",
            )
        )
    }

    private fun createArchiveWithContents(files: Map<String, String>) {
        createUnindexedFiles(files)


        files.forEach {
            val checksumPath = archiveTempDir.resolve(".archive/checksums").resolve(it.key + ".sha256").toPath()
            checksumPath.createParentDirectories()

            checksumPath.writeText(it.value.sha256())
        }
    }

    internal fun createUnindexedFiles(files: Map<String, String>) {
        files.forEach {
            val filePath = archiveTempDir.resolve(it.key).toPath()
            filePath.createParentDirectories()

            filePath.writeText(it.value)
        }
    }
}

fun String.sha256(): String {
    return hashString(this, "SHA-256")
}

private fun hashString(input: String, algorithm: String): String {
    return MessageDigest
        .getInstance(algorithm)
        .digest(input.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
}