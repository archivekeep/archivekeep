package org.archivekeep.cli

import org.archivekeep.cli.commands.Add
import org.archivekeep.cli.commands.Compare
import org.archivekeep.cli.commands.Push
import org.archivekeep.cli.commands.Status
import org.archivekeep.cli.workingarchive.WorkingArchive
import org.archivekeep.cli.workingarchive.openWorkingArchive
import org.archivekeep.core.repo.Repo
import org.archivekeep.core.repo.files.openFilesRepoOrNull
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Spec
import java.io.InputStream
import java.io.PrintWriter
import java.io.Reader
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

@Command(
    name = "archivekeep",

    mixinStandardHelpOptions = true,
    subcommands = [
        Add::class,
        Status::class,

        Compare::class,
        Push::class,
    ],
)
class MainCommand(
    private val workingDirectory: Path,
    private val inStream: InputStream= System.`in`
) {
    @Spec
    lateinit var spec: CommandLine.Model.CommandSpec

    // used for ascii doctor
    internal constructor(): this(Path.of("."))

    fun openCurrentArchive(): WorkingArchive {
        return openWorkingArchive(workingDirectory)
    }

    fun openOtherArchive(otherArchiveLocation: String): Repo {
        return openFilesRepoOrNull(workingDirectory.resolve(otherArchiveLocation)) ?: throw RuntimeException("not an archive")
    }

    val out: PrintWriter
        get () = spec.commandLine().out

    val `in` = inStream.bufferedReader()

    fun askForConfirmation(prompt: String): Boolean {
        while (true) {
            spec.commandLine().out.println("$prompt [y/n]: ")

            val answer = `in`.readLine() ?: throw RuntimeException("end of input")

            when (answer.lowercase()) {
                "y", "yes" -> return true
                "n", "no" -> return false
            }
        }
    }
}

fun main(args: Array<String>) {
    val cwd = Paths.get("").toAbsolutePath()

    val mainCommand = MainCommand(cwd)

    val result = CommandLine(mainCommand).execute(*args)

    exitProcess(result)
}
