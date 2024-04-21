package org.archivekeep.cli.commands

import org.archivekeep.cli.MainCommand
import org.archivekeep.core.operations.AddOperation
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Callable
import kotlin.io.path.pathString

@Command(
    name = "add",
    description = ["Adds files to archive."]
)
class Add : Callable<Int> {
    @Spec
    lateinit var spec: CommandSpec

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
        paramLabel = "globs",
        description = ["List of files (globs) to add"],
    )
    var globs: List<String> = emptyList()

    val out: PrintWriter
        get() = spec.commandLine().out

    override fun call(): Int {
        val currentArchive = mainCommand.openCurrentArchive()

        val rootRelativeGlobs = if (globs.isNotEmpty()) {
            globs
                .map { currentArchive.workingSubDirectory.resolve(it).normalize().pathString }
                .map { if (it == "") "." else it }
        } else {
            Collections.singletonList(".")
        }

        val result = AddOperation(
            subsetGlobs = rootRelativeGlobs,
            disableFilenameCheck = false,
            disableMovesCheck = disableMovesCheck
        ).prepare(currentArchive.repo)

        if (!doNotPrintPreparationSummary) {
            if(result.missingFiles.isNotEmpty()) {
                out.println("Missing indexed files not matched by add:")
                result.missingFiles.forEach { out.println("\t${currentArchive.fromArchiveToRelativePath(it)}") }
                out.println()
            }

            out.println("New files to be indexed:")
            result.newFiles.forEach { out.println("\t${currentArchive.fromArchiveToRelativePath(it)}") }
            out.println()

            if(result.moves.isNotEmpty()) {
                out.println("Files to be moved:")
                result.moves.forEach { out.println(
                    "\t${currentArchive.fromArchiveToRelativePath(it.from)} -> ${currentArchive.fromArchiveToRelativePath(it.to)}")
                }
                out.println()
            }
        }

        if (result.moves.isNotEmpty()) {
            if (mainCommand.askForConfirmation("\nDo want to perform move?")) {
                out.println("proceeding ...")

                result.executeMoves(
                    currentArchive.repo,
                    onMoveCompleted = { out.println("moved: ${currentArchive.fromArchiveToRelativePath(it.from)} to ${currentArchive.fromArchiveToRelativePath(it.to)}") }
                )
            }
        }

        if (result.newFiles.isNotEmpty()) {
            result.executeAddNewFiles(
                currentArchive.repo,
                onAddCompleted = { out.println("added: ${currentArchive.fromArchiveToRelativePath(it)}") }
            )
        }

        return 0
    }
}
