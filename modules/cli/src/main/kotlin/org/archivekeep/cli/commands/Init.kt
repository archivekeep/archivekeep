package org.archivekeep.cli.commands

import kotlinx.coroutines.runBlocking
import org.archivekeep.cli.MainCommand
import org.archivekeep.cli.workingarchive.init
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Spec
import java.io.PrintWriter
import java.util.concurrent.Callable

@Command(
    name = "init",
    description = ["Initializes a new archive."],
)
class Init : Callable<Int> {
    @Spec
    lateinit var spec: CommandSpec

    @ParentCommand
    private lateinit var mainCommand: MainCommand

    val out: PrintWriter
        get() = spec.commandLine().out

    override fun call(): Int =
        runBlocking(mainCommand.coroutineContext) {
            init(mainCommand.workingDirectory)

            return@runBlocking 0
        }
}
