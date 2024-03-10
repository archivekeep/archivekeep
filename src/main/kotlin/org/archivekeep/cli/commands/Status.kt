package org.archivekeep.cli.commands

import org.archivekeep.cli.MainCommand
import org.archivekeep.cli.workingarchive.openWorkingArchive
import org.archivekeep.core.operations.Status
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Parameters
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Spec
import java.io.PrintWriter
import java.util.Collections.singletonList
import java.util.concurrent.Callable
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

@Command(
    name = "status",
    description = ["Prints status of archive."],
)
class Status : Callable<Int> {
    @Spec
    lateinit var spec: CommandSpec

    @ParentCommand
    private lateinit var mainCommand: MainCommand

    @Parameters(
        paramLabel = "globs",
        description = ["Limit status to print files matching globs only"],
    )
    var globs: List<String> = emptyList()

    val out: PrintWriter
        get () = spec.commandLine().out

    override fun call(): Int {
        val currentArchive = mainCommand.openCurrentArchive()

        val rootRelativeGlobs = if (globs.isNotEmpty()) {
            globs
                .map { currentArchive.workingSubDirectory.resolve(it).normalize().pathString }
                .map { if (it == "") "." else it }
        } else {
            singletonList(".")
        }

        val result = Status(subsetGlobs = rootRelativeGlobs).execute(currentArchive.repo)

        if (result.newFiles.isNotEmpty()) {
            out.println("")
            out.println("Files not added to the archive:")

            result.newFiles.sorted().forEach {
                val relToWD = Path(it).relativeTo(currentArchive.workingSubDirectory)

                out.println("\t${relToWD.pathString}")
            }

            out.println()
        }

        if (globs.isNotEmpty()) {
            out.println("Files indexed in archive matching globs: ${result.storedFiles.size}")
        } else {
            out.println("Total indexed files in archive: ${result.storedFiles.size}")
        }

        return 0
    }
}