package org.archivekeep.cli.commands

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import org.archivekeep.cli.MainCommand
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateTextualProgressTracker
import org.archivekeep.utils.loading.LoadableWithProgress
import picocli.CommandLine.Command
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import picocli.CommandLine.ParentCommand
import picocli.CommandLine.Spec
import java.io.PrintWriter
import java.util.Collections
import java.util.concurrent.Callable
import kotlin.io.path.pathString

@Command(
    name = "add",
    description = ["Adds files to archive."],
)
class Add : Callable<Int> {
    @Spec
    lateinit var spec: CommandSpec

    @ParentCommand
    private lateinit var mainCommand: MainCommand

    @Option(
        names = ["--disable-moves-check"],
        description = ["do not check for moves or missing files"],
    )
    var disableMovesCheck: Boolean = false

    @Option(
        names = ["--do-not-print-preparation-summary"],
        description = ["do not print pre-execution summary"],
    )
    var doNotPrintPreparationSummary: Boolean = false

    @Parameters(
        paramLabel = "globs",
        description = ["List of files (globs) to add"],
    )
    var globs: List<String> = emptyList()

    val out: PrintWriter
        get() = spec.commandLine().out

    override fun call(): Int =
        runBlocking(mainCommand.coroutineContext) {
            val currentArchive = mainCommand.openCurrentArchive()

            val rootRelativeGlobs =
                if (globs.isNotEmpty()) {
                    globs
                        .map {
                            currentArchive.workingSubDirectory
                                .resolve(it)
                                .normalize()
                                .pathString
                        }.map { if (it == "") "." else it }
                } else {
                    Collections.singletonList(".")
                }

            val result =
                IndexUpdateProcedure(
                    subsetGlobs = rootRelativeGlobs,
                    disableFilenameCheck = false,
                    disableMovesCheck = disableMovesCheck,
                ).prepare(currentArchive.repo)
                    .transform {
                        when (it) {
                            is LoadableWithProgress.Failed -> throw it.throwable
                            is LoadableWithProgress.Loaded -> emit(it)
                            LoadableWithProgress.Loading -> {}
                            is LoadableWithProgress.LoadingProgress -> {}
                        }
                    }.first()
                    .value

            if (!doNotPrintPreparationSummary) {
                result.printSummary(
                    this@Add.out,
                    transformPath = { currentArchive.fromArchiveToRelativePath(it).toString() },
                )
                out.println()
            }

            val writter =
                IndexUpdateTextualProgressTracker(
                    out,
                    fromArchiveToRelativePath = currentArchive::fromArchiveToRelativePath,
                )

            if (result.moves.isNotEmpty()) {
                if (mainCommand.askForConfirmation("\nDo want to perform move?")) {
                    out.println("proceeding ...")

                    result.executeMovesReindex(
                        currentArchive.repo,
                        progressTrackers = arrayOf(writter),
                    )
                }
            }

            if (result.newFileNames.isNotEmpty()) {
                result.executeAddNewFiles(
                    currentArchive.repo,
                    progressTrackers = arrayOf(writter),
                )
            }

            return@runBlocking 0
        }
}
