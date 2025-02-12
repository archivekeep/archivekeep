package org.archivekeep.cli.commands

import kotlinx.coroutines.runBlocking
import org.archivekeep.cli.MainCommand
import org.archivekeep.files.operations.CompareOperation
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Parameters
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Spec
import java.io.PrintWriter
import java.util.concurrent.Callable

@Command(
    name = "compare",
    description = ["Compares current archive to other."],
)
class Compare : Callable<Int> {
    @Spec
    lateinit var spec: CommandSpec

    @ParentCommand
    private lateinit var mainCommand: MainCommand

    @Parameters(
        paramLabel = "other",
        description = ["Other archive"],
    )
    private lateinit var otherArchiveLocation: String

    val out: PrintWriter
        get() = spec.commandLine().out

    override fun call(): Int =
        runBlocking(mainCommand.coroutineContext) {
            val currentArchive = mainCommand.openCurrentArchive()
            val otherArchive = mainCommand.openOtherArchive(otherArchiveLocation)

            val result =
                CompareOperation().execute(
                    currentArchive.repo,
                    otherArchive,
                )

            result.printAll(out, "local", "remote")

            return@runBlocking 0
        }
}
