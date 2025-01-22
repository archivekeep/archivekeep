package org.archivekeep.cli.commands.shared

import org.archivekeep.cli.MainCommand
import org.archivekeep.cli.commands.mixins.SyncOptions
import org.archivekeep.core.operations.AdditiveRelocationsSyncStep
import org.archivekeep.core.operations.CompareOperation
import org.archivekeep.core.operations.DuplicationIncreasePresentButDisabledException
import org.archivekeep.core.operations.NewFilesSyncStep
import org.archivekeep.core.operations.RelocationsMoveApplySyncStep
import org.archivekeep.core.operations.RelocationsPresentButDisabledException
import org.archivekeep.core.operations.SyncLogger
import org.archivekeep.core.operations.SyncOperation
import org.archivekeep.core.repo.Repo
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

    val preparedSync =
        try {
            SyncOperation(syncOptions.syncMode).prepareFromComparison(comparisonResult)
        } catch (e: RelocationsPresentButDisabledException) {
            out.println(e.message)
            out.println("Enable relocations with --resolve-moves, or switch to --additive-duplicating mode")
            return 1
        } catch (e: DuplicationIncreasePresentButDisabledException) {
            out.println(e.message)
            out.println("Enable duplication increase with --allow-duplicate-increase, or switch to --additive-duplicating mode")
            return 1
        }

    if (preparedSync.isNoOp()) {
        out.println("No changes to $operationName")
    }

    preparedSync.execute(
        from,
        to,
        prompter = { step ->
            when (step) {
                is AdditiveRelocationsSyncStep ->
                    mainCommand.askForConfirmation("Do you want to $operationName in additive duplicating mode?")

                is RelocationsMoveApplySyncStep ->
                    mainCommand.askForConfirmation("Do you want to $operationName moves?")

                is NewFilesSyncStep ->
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
            },
    )

    return 0
}
