package org.archivekeep.cli.commands.shared

import org.archivekeep.cli.MainCommand
import org.archivekeep.cli.commands.mixins.SyncOptions
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.procedures.sync.DiscoveredAdditiveRelocationsGroup
import org.archivekeep.files.procedures.sync.DiscoveredNewFilesGroup
import org.archivekeep.files.procedures.sync.DiscoveredRelocationsMoveApplyGroup
import org.archivekeep.files.procedures.sync.RelocationSyncMode
import org.archivekeep.files.procedures.sync.SyncLogger
import org.archivekeep.files.procedures.sync.SyncProcedure
import org.archivekeep.files.repo.Repo
import java.io.PrintWriter

suspend fun executeSync(
    mainCommand: MainCommand,
    from: Repo,
    to: Repo,
    syncOptions: SyncOptions,
    out: PrintWriter,
    operationName: String,
    baseName: String,
    otherName: String,
): Int {
    val comparisonResult = CompareOperation().execute(from, to)

    comparisonResult.printAll(out, baseName, otherName)

    val preparedSync = SyncProcedure(syncOptions.syncMode).prepareFromComparison(comparisonResult)

    preparedSync.groups.filterIsInstance<DiscoveredRelocationsMoveApplyGroup>().forEach { relocationStep ->
        if (relocationStep.toIgnore.isNotEmpty()) {
            if (syncOptions.syncMode == RelocationSyncMode.Disabled) {
                out.println("Relocations disabled but present.")
                out.println("Enable relocations with --resolve-moves, or switch to --additive-duplicating mode")
            } else if (relocationStep.toIgnore.any { it.isIncreasingDuplicates }) {
                out.println("Duplicate increase is not enabled.")
                out.println("Enable duplication increase with --allow-duplicate-increase, or switch to --additive-duplicating mode")
            } else if (relocationStep.toIgnore.any { it.isDecreasingDuplicates }) {
                out.println("Duplicate decrease is not enabled.")
                out.println("Enable duplication decrease with --allow-duplicate-reduction, or switch to --additive-duplicating mode")
            } else {
                out.println("Ignored relocations detected.")
            }

            return 1
        }
    }

    if (preparedSync.isNoOp()) {
        out.println("No changes to $operationName")
    }

    val job =
        preparedSync.createJob(
            from,
            to,
            prompter = { step ->
                when (step) {
                    is DiscoveredAdditiveRelocationsGroup ->
                        mainCommand.askForConfirmation("Do you want to $operationName in additive duplicating mode?")

                    is DiscoveredRelocationsMoveApplyGroup ->
                        mainCommand.askForConfirmation("Do you want to $operationName moves?")

                    is DiscoveredNewFilesGroup ->
                        mainCommand.askForConfirmation("Do you want to $operationName new files?")
                }
            },
            logger =
                object : SyncLogger {
                    override fun onFileStored(filename: String) {
                        out.println("file stored: $filename")
                    }

                    override fun onFileMoved(
                        from: String,
                        to: String,
                    ) {
                        out.println("file moved: $from -> $to")
                    }

                    override fun onFileDeleted(filename: String) {
                        out.println("file deleted: $filename")
                    }
                },
        )

    job.run()

    return 0
}
