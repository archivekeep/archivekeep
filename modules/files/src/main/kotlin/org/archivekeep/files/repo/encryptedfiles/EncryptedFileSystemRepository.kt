package org.archivekeep.files.repo.encryptedfiles

import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.archivekeep.files.crypto.file.CryptoMetadata
import org.archivekeep.files.crypto.file.readCryptoStream
import org.archivekeep.files.crypto.file.writeCryptoStream
import org.archivekeep.files.exceptions.DestinationExists
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.files.repo.files.UnfinishedStoreCleanup
import org.archivekeep.files.repo.files.safeSubPath
import org.archivekeep.utils.InProgressHandler
import org.archivekeep.utils.coroutines.flowScopedToThisJob
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedJoseStorage
import org.archivekeep.utils.flows.logLoadableResourceLoad
import org.archivekeep.utils.io.watchRecursively
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.produceLoadable
import org.archivekeep.utils.loading.produceLoadableStateIn
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.safeFileReadWrite
import java.io.InputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID
import kotlin.io.path.createDirectory
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class EncryptedFileSystemRepository private constructor(
    val root: Path,
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    pollDispatcher: CoroutineDispatcher = Dispatchers.IO,
    throttlePauseDuration: Duration = 500.milliseconds,
) : Repo {
    private val scope = CoroutineScope(SupervisorJob() + CoroutineName("EncryptedFileSystemRepository") + stateDispatcher)

    private val encryptedFilesRoot = root.resolve("EncryptedFiles")
    private val vaultPath = root.resolve("vault.jwe")
    private val metadataPath = root.resolve("archive-metadata.json")

    private val inProgressHandler = InProgressHandler(scope)

    val vault =
        PasswordProtectedJoseStorage(
            vaultPath,
            Json.serializersModule.serializer(),
            defaultValueProducer = { EncryptedFileSystemRepositoryVaultContents(emptyList(), null, null) },
        )

    val indexRawFlow =
        encryptedFilesRoot
            .watchRecursively(ioDispatcher, pollDispatcher)
            .debounce(100.milliseconds)
            .map { "update" }
            .shareResourceIn(scope)
            .onStart { emit("start") }
            .conflate()
            .produceLoadable(
                ioDispatcher,
                "Repository index: $root",
                throttle = throttlePauseDuration,
            ) { index() }

    override val indexFlow: StateFlow<Loadable<RepoIndex>> =
        inProgressHandler
            .jobActiveOnIdleDelayedStart
            .flowScopedToThisJob { indexRawFlow }
            .logLoadableResourceLoad("Repository index: $root")
            .stateIn(scope)

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        metadataPath
            .produceLoadableStateIn(
                scope,
                watchDispatcher = ioDispatcher,
                workDispatcher = ioDispatcher,
                message = "Repository metadata: $root",
                throttle = throttlePauseDuration,
            ) {
                if (metadataPath.exists()) {
                    Json.decodeFromString<RepositoryMetadata>(metadataPath.readText())
                } else {
                    if (root.exists()) {
                        RepositoryMetadata()
                    } else {
                        throw RuntimeException("Something went wrong")
                    }
                }
            }

    override suspend fun index(): RepoIndex {
        val vaultContents = vault.data.firstLoadedOrNullOnErrorOrLocked()!!

        val files =
            Files
                .walk(encryptedFilesRoot)
                .asSequence()
                .filter { it.isRegularFile() && it.extension == "enc" }
                .toList()
                .map { file ->
                    val path = file.relativeTo(encryptedFilesRoot).invariantSeparatorsPathString.removeSuffix(".enc")

                    file.inputStream().use { inputStream ->
                        readCryptoStream(
                            inputStream,
                            ECDSAVerifier(vaultContents.currentFileSigningKey!!.toECKey().toECPublicKey()),
                            ECDHDecrypter(
                                vaultContents.currentFileEncryptionKey!!.toECKey().toECPrivateKey(),
                            ),
                        ) { plainMetadata, contents ->
                            RepoIndex.File(
                                path = path,
                                size = plainMetadata.size,
                                checksumSha256 = plainMetadata.checksumSha256,
                            )
                        }
                    }
                }.sortedBy { it.path }
                .toList()

        return RepoIndex(files)
    }

    override suspend fun move(
        from: String,
        to: String,
    ) {
        val fromEnc = encryptedFilesRoot.resolve(safeSubPath("$from.enc"))
        val toEnc = encryptedFilesRoot.resolve(safeSubPath("$to.enc"))

        withContext(Dispatchers.IO) {
            val dstPath = encryptedFilesRoot.resolve(safeSubPath(toEnc))
            if (dstPath.exists()) {
                throw DestinationExists(to)
            }

            dstPath.createParentDirectories()
            fromEnc.moveTo(toEnc)
        }
    }

    override suspend fun <T> open(
        filename: String,
        block: suspend (ArchiveFileInfo, InputStream) -> T,
    ): T {
        val vaultContents = vault.data.firstLoadedOrNullOnErrorOrLocked()!!

        return encryptedFilesRoot
            .resolve(safeSubPath("$filename.enc"))
            .inputStream()
            .use { inputStream ->
                readCryptoStream(
                    inputStream,
                    ECDSAVerifier(vaultContents.currentFileSigningKey!!.toECKey().toECPublicKey()),
                    ECDHDecrypter(
                        vaultContents.currentFileEncryptionKey!!.toECKey().toECPrivateKey(),
                    ),
                ) { plainMetadata, contents ->
                    block(
                        ArchiveFileInfo(plainMetadata.size, plainMetadata.checksumSha256),
                        contents,
                    )
                }
            }
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
    ) {
        withContext(ioDispatcher) {
            val cleanup = UnfinishedStoreCleanup()

            try {
                inProgressHandler.onStart(filename)

                val vaultContents = vault.data.firstLoadedOrNullOnErrorOrLocked()
                val dstPath = encryptedFilesRoot.resolve(safeSubPath("$filename.enc"))

                dstPath.createParentDirectories()

                val os =
                    try {
                        Files.newOutputStream(
                            dstPath,
                            StandardOpenOption.CREATE_NEW,
//                    StandardOpenOption.SYNC,
                            StandardOpenOption.WRITE,
                        )
                    } catch (e: FileAlreadyExistsException) {
                        throw DestinationExists(filename, cause = e)
                    }

                cleanup.files.add(dstPath)

                os.use {
                    coroutineScope {
                        val pipedInputStream =
                            verifyingStreamViaBackgroundCoroutine(
                                stream,
                                monitor,
                                info.checksumSha256,
                                blockingIOPopulateDispatcher = ioDispatcher,
                                checksumComputeDispatcher = computeDispatcher,
                            )

                        runInterruptible {
                            writeCryptoStream(
                                CryptoMetadata.Plain(
                                    size = info.length,
                                    checksumSha256 = info.checksumSha256,
                                ),
                                vaultContents!!.currentFileSigningKey!!,
                                ECDHEncrypter(vaultContents.currentFileEncryptionKey!!.toECKey().toECPublicKey()),
                                pipedInputStream,
                                os,
                            )
                        }

                        cleanup.cancel()
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    if (cleanup.files.isNotEmpty()) {
                        println("Not completed successfully, cleaning up: ${cleanup.files}")
                        cleanup.runPremature()
                    }
                    inProgressHandler.onEnd(filename)
                }
            }
        }
    }

    override suspend fun delete(filename: String) {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getMetadata(): RepositoryMetadata =
        withContext(Dispatchers.IO) {
            if (metadataPath.exists()) {
                metadataPath.inputStream().use {
                    Json.decodeFromStream<RepositoryMetadata>(it)
                }
            } else {
                if (root.exists()) {
                    RepositoryMetadata()
                } else {
                    throw RuntimeException("Something went wrong")
                }
            }
        }

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        safeFileReadWrite(metadataPath) { oldString ->
            val old = oldString?.let { Json.decodeFromString(oldString) } ?: RepositoryMetadata()

            Json.encodeToString(transform(old))
        }
    }

    companion object {
        suspend operator fun invoke(
            path: Path,
            password: String,
            stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
            computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
            pollDispatcher: CoroutineDispatcher = Dispatchers.IO,
            throttlePauseDuration: Duration = 500.milliseconds,
        ) = EncryptedFileSystemRepository(
            path,
            stateDispatcher = stateDispatcher,
            computeDispatcher = computeDispatcher,
            ioDispatcher = ioDispatcher,
            pollDispatcher = pollDispatcher,
            throttlePauseDuration,
        ).apply { vault.unlock(password) }

        suspend fun create(
            path: Path,
            password: String,
        ): EncryptedFileSystemRepository {
            val dirList = path.toFile().list()

            if (dirList?.isNotEmpty() == true) {
                throw RuntimeException("Already exists or destination is not empty")
            }

            path.resolve("EncryptedFiles").createDirectory()

            return EncryptedFileSystemRepository(path)
                .apply {
                    vault.create(password)

                    val ecJWK =
                        ECKeyGenerator(Curve.P_256)
                            .keyID(UUID.randomUUID().toString())
                            .generate()

                    vault.updateData {
                        EncryptedFileSystemRepositoryVaultContents(
                            listOf(ecJWK),
                            ecJWK.keyID,
                            ecJWK.keyID,
                        )
                    }
                }
        }

        suspend fun isRepository(path: Path): Boolean =
            EncryptedFileSystemRepository(path)
                .vault
                .autoloadFlow
                .dropWhile { it is PasswordProtectedJoseStorage.State.NotInitialized }
                .map { it !is PasswordProtectedJoseStorage.State.NotExisting }
                .first()
    }
}
