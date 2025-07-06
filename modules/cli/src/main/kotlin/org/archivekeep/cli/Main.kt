package org.archivekeep.cli

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.archivekeep.cli.commands.Add
import org.archivekeep.cli.commands.Compare
import org.archivekeep.cli.commands.Init
import org.archivekeep.cli.commands.Pull
import org.archivekeep.cli.commands.Push
import org.archivekeep.cli.commands.Status
import org.archivekeep.cli.utils.SignalInterruptCancellationException
import org.archivekeep.cli.workingarchive.WorkingArchive
import org.archivekeep.cli.workingarchive.openWorkingArchive
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.files.FilesRepo
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.files.repo.remote.grpc.Options
import org.archivekeep.files.repo.remote.grpc.grpcPrefix
import org.archivekeep.files.repo.remote.grpc.isNotAuthorized
import org.archivekeep.files.repo.remote.grpc.openGrpcArchive
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.IExecutionExceptionHandler
import picocli.CommandLine.Model
import picocli.CommandLine.Option
import picocli.CommandLine.Spec
import sun.misc.Signal
import java.io.InputStream
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.exitProcess

@Command(
    name = "archivekeep",
    mixinStandardHelpOptions = true,
    subcommands = [
        Init::class,

        Add::class,
        Status::class,

        Compare::class,
        Push::class,
        Pull::class,
    ],
)
class MainCommand(
    val workingDirectory: Path,
    val coroutineContext: CoroutineContext,
    private val inStream: InputStream = System.`in`,
) {
    @Spec
    lateinit var spec: Model.CommandSpec

    @Option(
        names = ["--insecure"],
        description = ["allow insecure connection"],
        scope = CommandLine.ScopeType.INHERIT,
    )
    var insecure: Boolean = false

    // used for ascii doctor
    internal constructor() : this(Path.of("."), EmptyCoroutineContext)

    fun openCurrentArchive(): WorkingArchive = openWorkingArchive(workingDirectory)

    suspend fun openOtherArchive(otherArchiveLocation: String): Repo {
        if (otherArchiveLocation.startsWith(grpcPrefix)) {
            val options =
                Options(
                    credentials = null,
                    insecure = insecure,
                )

            try {
                return openGrpcArchive(otherArchiveLocation, options)
            } catch (e: Exception) {
                if (e.isNotAuthorized()) {
                    val username = askForText("Please, enter username")
                    val password = askForText("Please, enter password")

                    val optionsWithCredentials =
                        options.copy(
                            credentials = BasicAuthCredentials(username, password),
                        )

                    return openGrpcArchive(otherArchiveLocation, optionsWithCredentials)
                } else {
                    throw e
                }
            }
        }

        return FilesRepo.openOrNull(workingDirectory.resolve(otherArchiveLocation))
            ?: throw RuntimeException("not an archive")
    }

    val out: PrintWriter
        get() = spec.commandLine().out

    val `in` = inStream.bufferedReader()

    suspend fun askForConfirmation(prompt: String): Boolean {
        while (true) {
            val answer =
                withContext(Dispatchers.IO) {
                    spec.commandLine().out.println("$prompt [y/n]: ")

                    // don't inherit parent context, because this is non-cancellable non-interruptible
                    CoroutineScope(Dispatchers.IO + Job())
                        .async {
                            `in`.readLine() ?: throw RuntimeException("end of input")
                        }.await()
                }

            when (answer.lowercase()) {
                "y", "yes" -> return true
                "n", "no" -> return false
            }
        }
    }

    suspend fun askForText(prompt: String): String {
        val answer =
            withContext(Dispatchers.IO) {
                spec.commandLine().out.println("$prompt: ")

                // don't inherit parent context, because this is non-cancellable non-interruptible
                CoroutineScope(Dispatchers.IO + Job())
                    .async {
                        `in`.readLine() ?: throw RuntimeException("end of input")
                    }.await()
            }

        return answer.trimEnd()
    }
}

fun main(args: Array<String>) {
    val supervisorJob = SupervisorJob()

    Signal.handle(Signal("INT")) {
        runBlocking {
            supervisorJob.cancel(SignalInterruptCancellationException())
            supervisorJob.join()
        }
    }

    val cwd = Paths.get("").toAbsolutePath()

    val mainCommand = MainCommand(cwd, supervisorJob)

    val result =
        CommandLine(mainCommand)
            .apply {
                setExecutionExceptionHandler(
                    object : IExecutionExceptionHandler {
                        override fun handleExecutionException(
                            ex: java.lang.Exception,
                            commandLine: CommandLine?,
                            parseResult: CommandLine.ParseResult?,
                        ): Int {
                            if (ex is SignalInterruptCancellationException) {
                                println("\n\nAborted by user: $ex")
                                return 1
                            }

                            throw ex
                        }
                    },
                )
            }.execute(*args)

    exitProcess(result)
}
