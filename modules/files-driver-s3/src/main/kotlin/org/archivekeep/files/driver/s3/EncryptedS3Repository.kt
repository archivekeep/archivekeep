package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.copyObject
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.headObject
import aws.sdk.kotlin.services.s3.listObjects
import aws.sdk.kotlin.services.s3.model.ChecksumAlgorithm
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.MetadataDirective
import aws.sdk.kotlin.services.s3.model.NoSuchBucket
import aws.sdk.kotlin.services.s3.model.NoSuchKey
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.Object
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.content.toInputStream
import aws.smithy.kotlin.runtime.net.url.Url
import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSAVerifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.archivekeep.files.crypto.file.EncryptedFileMetadata
import org.archivekeep.files.crypto.file.readCryptoStream
import org.archivekeep.files.crypto.file.writeCryptoStream
import org.archivekeep.files.crypto.parseVerifyDecodeJWS
import org.archivekeep.files.crypto.signAsJWS
import org.archivekeep.files.encryption.EncryptedFileSystemRepositoryVaultContents
import org.archivekeep.files.encryption.verifyingStreamViaBackgroundCoroutine
import org.archivekeep.files.exceptions.DestinationExists
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedCustomJoseStorage
import org.archivekeep.utils.exceptions.WrongCredentialsException
import org.archivekeep.utils.fromBase64ToHex
import org.archivekeep.utils.fromHexToBase64
import org.archivekeep.utils.loading.AutoRefreshLoadableFlow
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.firstLoadedOrThrowOnErrorOrLocked
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Date

@Serializable
private data class SignedObjectMetadata(
    val objectChecksumSha256: String,
    val plainFileMetadata: EncryptedFileMetadata.Plain,
)

class EncryptedS3Repository private constructor(
    val endpoint: URI,
    val region: String,
    val credentialsProvider: CredentialsProvider,
    val bucketName: String,
    val sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Repo {
    private lateinit var s3Client: S3Client

    private val scope = CoroutineScope(sharingDispatcher + SupervisorJob())

    private val contentsLastChangeFlow = MutableStateFlow(Date())
    private val metadataLastChangeFlow = MutableStateFlow(Date())

    val vault =
        PasswordProtectedCustomJoseStorage(
            Json.serializersModule.serializer(),
            defaultValueProducer = { EncryptedFileSystemRepositoryVaultContents(emptyList(), null, null) },
            reader = { readVaultContents() },
            writer = { updater ->
                // TODO: maybe real locking?

                val oldData =
                    try {
                        s3Client.getObject(
                            GetObjectRequest {
                                bucket = bucketName
                                key = VAULT_LOCATION
                            },
                        ) { response ->
                            response.body?.toByteArray()
                        }
                    } catch (e: NoSuchKey) {
                        null
                    }

                val newData = updater(oldData)

                s3Client.putObject {
                    bucket = bucketName
                    key = VAULT_LOCATION

                    body = ByteStream.fromBytes(newData)
                }

                newData
            },
        )

    private suspend fun readVaultContents() =
        try {
            s3Client.getObject(
                GetObjectRequest {
                    bucket = bucketName
                    key = VAULT_LOCATION
                },
            ) { response ->
                response.body?.toByteArray()
            }
        } catch (e: NoSuchKey) {
            null
        }

    suspend fun init() {
        s3Client =
            S3Client.fromEnvironment {
                endpointUrl = Url.parse(endpoint.toString())
                region = this@EncryptedS3Repository.region
                credentialsProvider = this@EncryptedS3Repository.credentialsProvider
                forcePathStyle = true
            }

        try {
            s3Client.listObjects {
                bucket = bucketName
                maxKeys = 1
            }
        } catch (e: NoSuchBucket) {
            throw e
        } catch (e: S3Exception) {
            throw WrongCredentialsException(cause = e)
        }
    }

    private val indexResource =
        AutoRefreshLoadableFlow(
            scope,
            ioDispatcher,
            updateTriggerFlow = contentsLastChangeFlow,
        ) {
            val files =
                coroutineScope {
                    val vaultContents = vault.data.firstLoadedOrNullOnErrorOrLocked()!!

                    var allItems = emptyList<Object>()
                    var nextMarker: String? = null

                    do {
                        val response =
                            s3Client
                                .listObjects {
                                    bucket = bucketName
                                    prefix = ENCRYPTED_FILES_PREFIX
                                    marker = nextMarker
                                }

                        allItems = allItems + (response.contents ?: emptyList())

                        nextMarker =
                            if (response.isTruncated == true) {
                                allItems.last().key
                            } else {
                                null
                            }
                    } while (nextMarker != null)

                    allItems
                        .map {
                            async {
                                try {
                                    val plainMetadata =
                                        run {
                                            val response =
                                                s3Client.headObject {
                                                    bucket = bucketName
                                                    key = it.key
                                                }

                                            val objectChecksum = response.checksumSha256!!
                                            val signedObjectMetadata =
                                                parseVerifyDecodeJWS<SignedObjectMetadata>(
                                                    response.metadata!![SIGNED_METADATA]!!,
                                                    ECDSAVerifier(vaultContents.currentFileSigningKey!!.toECKey().toECPublicKey()),
                                                )

                                            if (objectChecksum != signedObjectMetadata.objectChecksumSha256) {
                                                throw RuntimeException("Integrity corrupted of ${it.key!!.toFilename()}")
                                            }

                                            signedObjectMetadata.plainFileMetadata
                                        }

                                    RepoIndex.File(
                                        it.key!!.toFilename(),
                                        plainMetadata.size,
                                        plainMetadata.checksumSha256,
                                    )
                                } catch (e: Throwable) {
                                    // TODO: show to UI
                                    println("Index get error: $e")
                                    e.printStackTrace()
                                    null
                                }
                            }
                        }.awaitAll()
                }

            RepoIndex(files.filterNotNull())
        }

    override val indexFlow: StateFlow<Loadable<RepoIndex>> = indexResource.stateFlow

    private val metadataResource =
        AutoRefreshLoadableFlow(
            scope,
            ioDispatcher,
            updateTriggerFlow = metadataLastChangeFlow,
        ) {
            readMetadataFromS3() ?: RepositoryMetadata()
        }

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> = metadataResource.stateFlow

    suspend fun unlock(password: String) {
        vault.unlock(password)
    }

    override suspend fun index(): RepoIndex = indexResource.getFreshAndUpdateState()

    override suspend fun move(
        from: String,
        to: String,
    ) {
        try {
            checkNotExists(to)

            s3Client.copyObject {
                copySource = URLEncoder.encode("$bucketName/${from.toKey()}", StandardCharsets.UTF_8.toString())

                bucket = bucketName
                key = to.toKey()
            }

            s3Client.deleteObject {
                bucket = bucketName
                key = from.toKey()
            }
        } finally {
            contentsLastChangeFlow.update { Date() }
        }
    }

    override suspend fun <T> open(
        filename: String,
        block: suspend (ArchiveFileInfo, InputStream) -> T,
    ): T {
        val vaultContents = vault.data.firstLoadedOrNullOnErrorOrLocked()!!

        return s3Client.getObject(
            GetObjectRequest {
                bucket = bucketName
                key = filename.toKey()
            },
        ) { response ->
            readCryptoStream(
                response.body!!.toInputStream(),
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

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
    ) {
        try {
            checkNotExists(filename)

            withContext(ioDispatcher) {
                val vaultContents = vault.data.firstLoadedOrThrowOnErrorOrLocked()

                coroutineScope {
                    val pipedInputStream =
                        verifyingStreamViaBackgroundCoroutine(
                            stream,
                            monitor,
                            info.checksumSha256,
                            blockingIOPopulateDispatcher = ioDispatcher,
                            checksumComputeDispatcher = computeDispatcher,
                        )

                    val encryptedInputStream = PipedInputStream()
                    val encryptedOutputStream = PipedOutputStream(encryptedInputStream)

                    val plainMetadata =
                        EncryptedFileMetadata.Plain(
                            size = info.length,
                            checksumSha256 = info.checksumSha256,
                        )

                    val encryptedWriter =
                        launch {
                            runInterruptible {
                                writeCryptoStream(
                                    plainMetadata,
                                    vaultContents.currentFileSigningKey!!,
                                    ECDHEncrypter(vaultContents.currentFileEncryptionKey!!.toECKey().toECPublicKey()),
                                    pipedInputStream,
                                    encryptedOutputStream,
                                )
                            }
                        }

                    val digest = MessageDigest.getInstance("SHA-256")
                    val digestInputStream = DigestInputStream(encryptedInputStream, digest)

                    try {
                        val newObject =
                            s3Client.putObject {
                                bucket = bucketName
                                key = filename.toKey()

                                body = interruptibleSourceFromInputStream(digestInputStream)

                                checksumAlgorithm = ChecksumAlgorithm.Sha256
                            }

                        val digestHex = digest.digest().toHexString()
                        if (newObject.checksumSha256?.fromBase64ToHex() != digestHex) {
                            throw RuntimeException("Checksum doesn't match")
                        }

                        val signedMetadataJSON = Json.encodeToString(SignedObjectMetadata(digestHex.fromHexToBase64(), plainMetadata))

                        // TODO: create TMP file and don't use copy
                        s3Client.copyObject {
                            copySource = "$bucketName/${filename.toKey()}"

                            bucket = bucketName
                            key = filename.toKey()

                            metadataDirective = MetadataDirective.Replace

                            metadata = mapOf(SIGNED_METADATA to signAsJWS(signedMetadataJSON, vaultContents.currentFileSigningKey!!))
                        }
                    } catch (e: Throwable) {
                        throw RuntimeException(e)
                    }

                    encryptedWriter.join()
                }
            }
        } finally {
            contentsLastChangeFlow.update { Date() }
        }
    }

    private suspend fun checkNotExists(filename: String) {
        try {
            s3Client.headObject {
                bucket = bucketName
                key = filename.toKey()
            }

            throw DestinationExists(filename)
        } catch (e: NotFound) {
            return
        }
    }

    override suspend fun delete(filename: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getMetadata() = metadataResource.getFreshAndUpdateState()

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        // TODO: lock metadata for concurrent update

        // TODO: encrypt or sign metadata

        try {
            val old = readMetadataFromS3() ?: RepositoryMetadata()
            val newMetadataBytes = Json.encodeToString(transform(old)).toByteArray()

            s3Client.putObject {
                bucket = bucketName
                key = METADATA_JSON_KEY

                body = newMetadataBytes.inputStream().asByteStream(newMetadataBytes.size.toLong())
            }
        } finally {
            metadataLastChangeFlow.update { Date() }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun readMetadataFromS3(): RepositoryMetadata? =
        try {
            s3Client
                .getObject(
                    GetObjectRequest {
                        bucket = bucketName
                        key = METADATA_JSON_KEY
                    },
                ) {
                    Json.decodeFromStream(it.body!!.toInputStream())
                }
        } catch (e: NoSuchKey) {
            null
        }

    companion object {
        private const val METADATA_JSON_KEY = "metadata.json"

        private const val SIGNED_METADATA = "signed-metadata"

        private const val ENCRYPTED_FILES_PREFIX = "encrypted-files/"

        private const val VAULT_LOCATION = "vault.jwe"

        fun String.toKey() = "$ENCRYPTED_FILES_PREFIX$this.enc"

        fun String.toFilename() = this.removePrefix(ENCRYPTED_FILES_PREFIX).removeSuffix(".enc")

        suspend fun create(
            endpoint: URI,
            region: String,
            credentialsProvider: CredentialsProvider,
            bucketName: String,
            password: String,
            sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = EncryptedS3Repository(
            endpoint,
            region,
            credentialsProvider,
            bucketName,
            sharingDispatcher,
            ioDispatcher,
        ).apply {
            init()

            // create them from defaults
            updateMetadata { it }

            vault.create(password)

            vault.updateData {
                EncryptedFileSystemRepositoryVaultContents.generateNew()
            }
        }

        suspend fun open(
            endpoint: URI,
            region: String,
            credentialsProvider: CredentialsProvider,
            bucketName: String,
            sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = EncryptedS3Repository(
            endpoint,
            region,
            credentialsProvider,
            bucketName,
            sharingDispatcher,
            ioDispatcher,
        ).apply {
            init()

            readMetadataFromS3() ?: throw S3LocationNotInitializedAsRepositoryException()
            readVaultContents() ?: throw S3LocationNotInitializedAsRepositoryException()
        }

        suspend fun openAndUnlock(
            endpoint: URI,
            region: String,
            credentialsProvider: CredentialsProvider,
            bucketName: String,
            password: String,
            sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = open(
            endpoint,
            region,
            credentialsProvider,
            bucketName,
            sharingDispatcher,
            ioDispatcher,
        ).apply {
            unlock(password)
        }
    }
}
