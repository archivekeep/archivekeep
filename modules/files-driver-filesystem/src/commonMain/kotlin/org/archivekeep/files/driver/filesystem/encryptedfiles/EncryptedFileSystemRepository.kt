package org.archivekeep.files.driver.filesystem.encryptedfiles

import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSAVerifier
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
import org.archivekeep.files.api.exceptions.DestinationExists
import org.archivekeep.files.api.repository.ARCHIVE_METADATA_FILENAME
import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.ENCRYPTED_FILES_DIRECTORY
import org.archivekeep.files.api.repository.ENCRYPTED_REPOSITORY_TYPE
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.api.repository.VAULT_FILENAME
import org.archivekeep.files.crypto.EncryptedFileSystemRepositoryVaultContents
import org.archivekeep.files.crypto.file.EncryptedFileMetadata
import org.archivekeep.files.crypto.file.readCryptoStream
import org.archivekeep.files.crypto.file.writeCryptoStream
import org.archivekeep.files.crypto.verifyingStreamViaBackgroundCoroutine
import org.archivekeep.files.driver.filesystem.files.safeSubPath
import org.archivekeep.utils.coroutines.InProgressHandler
import org.archivekeep.utils.coroutines.flowScopedToThisJob
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedJoseStorageInFile
import org.archivekeep.utils.flows.logLoadableResourceLoad
import org.archivekeep.utils.io.AutomaticFileCleanup
import org.archivekeep.utils.io.createTmpFileForWrite
import org.archivekeep.utils.io.moveTmpToDestination
import org.archivekeep.utils.io.watchRecursively
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.produceLoadable
import org.archivekeep.utils.loading.produceLoadableStateIn
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.safeFileReadWrite
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
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

    private val encryptedFilesRoot = root.resolve(ENCRYPTED_FILES_DIRECTORY)
    private val vaultPath = root.resolve(VAULT_FILENAME)
    private val metadataPath = root.resolve(ARCHIVE_METADATA_FILENAME)

    private val inProgressHandler = InProgressHandler(scope)

    val vault =
        PasswordProtectedJoseStorageInFile(
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
            val cleanup = AutomaticFileCleanup()

            try {
                inProgressHandler.onStart(filename)

                val vaultContents = vault.data.firstLoadedOrNullOnErrorOrLocked()
                val dstPath = encryptedFilesRoot.resolve(safeSubPath("$filename.enc"))

                if (dstPath.exists()) {
                    throw DestinationExists(filename)
                }

                dstPath.createParentDirectories()

                val (dstTmpFilePath, os) = createTmpFileForWrite(dstPath, Files::newOutputStream)

                cleanup.files.add(dstTmpFilePath)

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
                                EncryptedFileMetadata.Plain(
                                    size = info.length,
                                    checksumSha256 = info.checksumSha256,
                                ),
                                vaultContents!!.currentFileSigningKey!!,
                                ECDHEncrypter(vaultContents.currentFileEncryptionKey!!.toECKey().toECPublicKey()),
                                pipedInputStream,
                                os,
                            )
                        }
                    }
                }

                moveTmpToDestination(dstTmpFilePath, dstPath)

                cleanup.cancel()
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
        suspend fun openAndUnlock(
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

            path.resolve(ENCRYPTED_FILES_DIRECTORY).createDirectory()

            return EncryptedFileSystemRepository(path)
                .apply {
                    vault.create(password)

                    vault.updateData { EncryptedFileSystemRepositoryVaultContents.generateNew() }

                    updateMetadata {
                        it.copy(
                            repositoryType = ENCRYPTED_REPOSITORY_TYPE,
                        )
                    }
                }
        }

        suspend fun isRepository(path: Path): Boolean =
            EncryptedFileSystemRepository(path)
                .vault
                .autoloadFlow
                .dropWhile { it is PasswordProtectedJoseStorageInFile.State.NotInitialized }
                .map { it !is PasswordProtectedJoseStorageInFile.State.NotExisting }
                .first()
    }
}
