package org.archivekeep.cli

import org.archivekeep.cli.commands.Add
import org.archivekeep.cli.commands.Status
import org.archivekeep.cli.workingarchive.WorkingArchive
import org.archivekeep.cli.workingarchive.openWorkingArchive
import picocli.CommandLine
import picocli.CommandLine.Command
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

@Command(
    name = "archivekeep",

    mixinStandardHelpOptions = true,
    subcommands = [
        Add::class,
        Status::class,
    ],
)
class MainCommand(
    private val workingDirectory: Path
) {
    // used for ascii doctor
    internal constructor(): this(Path.of("."))

    fun openCurrentArchive(): WorkingArchive {
        return openWorkingArchive(workingDirectory)
    }
}

fun main(args: Array<String>) {
    val cwd = Paths.get("").toAbsolutePath()

    val mainCommand = MainCommand(cwd)

    val result = CommandLine(mainCommand).execute(*args)

    exitProcess(result)
}