package org.archivekeep.cli.commands

import kotlinx.coroutines.runBlocking
import org.archivekeep.cli.MainCommand
import org.archivekeep.cli.commands.mixins.SyncOptions
import org.archivekeep.cli.commands.shared.executeSync
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Parameters
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Spec
import java.io.PrintWriter
import java.util.concurrent.Callable

@Command(
    name = "pull",
    description = ["Pulls changes to current archive from other repository."],
)
class Pull : Callable<Int> {
    @Spec
    lateinit var spec: CommandSpec

    @ParentCommand
    private lateinit var mainCommand: MainCommand

    @Parameters(
        paramLabel = "other",
        description = ["Other repository location"],
    )
    private lateinit var otherArchiveLocation: String

    @Mixin
    private lateinit var syncOptions: SyncOptions

    val out: PrintWriter
        get() = spec.commandLine().out

    override fun call(): Int =
        runBlocking(mainCommand.coroutineContext) {
            val currentArchive = mainCommand.openCurrentArchive()
            val otherArchive = mainCommand.openOtherArchive(otherArchiveLocation)

            executeSync(
                mainCommand,
                otherArchive,
                currentArchive.repo,
                syncOptions,
                out,
                "pull",
                "other",
                "current",
            )
        }
}
