package org.archivekeep.cli.commands

import org.archivekeep.cli.MainCommand
import picocli.CommandLine.*
import java.util.concurrent.Callable

@Command(
    name = "add",
    description = ["Adds files to archive."]
)
class Add : Callable<Int> {
    @ParentCommand
    private lateinit var mainCommand: MainCommand

    @Option(
        names = ["--disable-moves-check"],
        description = ["do not check for moves or missing files"]
    )
    var disableMovesCheck: Boolean = false

    @Option(
        names = ["--do-not-print-preparation-summary"],
        description = ["do not print pre-execution summary"]
    )
    var doNotPrintPreparationSummary: Boolean = false

    @Parameters(
        arity = "1..*"
    )
    lateinit var paths: List<String>

    override fun call(): Int {
        TODO("Not yet implemented")
    }
}