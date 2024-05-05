package org.archivekeep.cli.commands

import org.archivekeep.cli.MainCommand
import org.archivekeep.core.operations.*
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.io.PrintWriter
import java.util.concurrent.Callable

@Command(
    name = "push",
    description = ["Pushes changes from current archive to other repository."],
)
class Push : Callable<Int> {
    @Spec
    lateinit var spec: CommandSpec

    @ParentCommand
    private lateinit var mainCommand: MainCommand

    @Parameters(
        paramLabel = "other",
        description = ["Other repository location"],
    )
    private lateinit var otherArchiveLocation: String

    @Option(
        names = ["--resolve-moves"],
        description = ["resolves added files against checksums of existing remote files"]
    )
    var resolveMoves: Boolean = false

    @Option(
        names = ["--allow-duplicate-increase"],
        description = ["allow to increase duplication of existing files"]
    )
    var allowDuplicateIncrease: Boolean = false

    @Option(
        names = ["--allow-duplicate-reduction"],
        description = ["allow to decrease duplication of existing files"]
    )
    var allowDuplicateReduction: Boolean = false

    @Option(
        names = ["--additive-duplicating"],
        description = ["push all new local files to remote without resolving moves or checking for duplication increase"]
    )
    var additiveDuplicating: Boolean = false

    val out: PrintWriter
        get() = spec.commandLine().out

    override fun call(): Int {
        val currentArchive = mainCommand.openCurrentArchive()
        val otherArchive = mainCommand.openOtherArchive(otherArchiveLocation)

        val syncMode = when {
            additiveDuplicating -> RelocationSyncMode.AdditiveDuplicating
            resolveMoves -> RelocationSyncMode.Move(
                allowDuplicateIncrease = allowDuplicateIncrease,
                allowDuplicateReduction = allowDuplicateReduction
            )

            else -> RelocationSyncMode.Disabled
        }


        val preparedSync = try {
            SyncOperation(syncMode).prepare(currentArchive.repo, otherArchive)
        } catch (e: RelocationsPresentButDisabledException) {
            out.println(e.message)
            out.println("Enable relocations with --resolve-moves, or switch to --additive-duplicating mode")
            return 1
        } catch (e: DuplicationIncreasePresentButDisabledException) {
            out.println(e.message)
            out.println("Enable duplication increase with --allow-duplicate-increase, or switch to --additive-duplicating mode")
            return 1
        }

        preparedSync.execute(
            currentArchive.repo,
            otherArchive,
            prompter = { step ->
                when (step) {
                    is AdditiveRelocationsSyncStep ->
                        mainCommand.askForConfirmation("Do you want to perform additive duplicating?")

                    is RelocationsMoveApplySyncStep ->
                        mainCommand.askForConfirmation("Do you want to perform moves?")

                    is NewFilesSyncStep ->
                        mainCommand.askForConfirmation("Do you want to push new files?")
                }
            },
            logger = object : SyncLogger {
                override fun onFileStored(filename: String) {
                    out.println("file stored: $filename")
                }

                override fun onFileMoved(from: String, to: String) {
                    out.println("file moved: $from -> $to")
                }
            }
        )

        return 0
    }
}
