package org.archivekeep.files.driver.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.copyObject
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.headObject
import aws.sdk.kotlin.services.s3.listObjects
import aws.sdk.kotlin.services.s3.model.ChecksumMode
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.NoSuchBucket
import aws.sdk.kotlin.services.s3.model.NoSuchKey
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.Object
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.toInputStream
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.archivekeep.files.api.exceptions.ChecksumMismatch
import org.archivekeep.files.api.exceptions.DestinationExists
import org.archivekeep.files.api.repository.ARCHIVE_METADATA_FILENAME
import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.utils.exceptions.WrongCredentialsException
import org.archivekeep.utils.fromBase64ToHex
import org.archivekeep.utils.fromHexToBase64
import org.archivekeep.utils.hashing.sha256
import org.archivekeep.utils.loading.AutoRefreshLoadableFlow
import org.archivekeep.utils.loading.Loadable
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Date

class S3Repository private constructor(
    val endpoint: URI,
    val region: String,
    val credentialsProvider: CredentialsProvider,
    val bucketName: String,
    val sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Repo {
    private lateinit var s3Client: S3Client

    private val scope = CoroutineScope(sharingDispatcher + SupervisorJob())

    private val contentsLastChangeFlow = MutableStateFlow(Date())
    private val metadataLastChangeFlow = MutableStateFlow(Date())

    suspend fun init() {
        s3Client =
            S3Client.fromEnvironment {
                endpointUrl = Url.parse(endpoint.toString())
                region = this@S3Repository.region
                credentialsProvider = this@S3Repository.credentialsProvider
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
                    var allItems = emptyList<Object>()
                    var nextMarker: String? = null

                    do {
                        val response =
                            s3Client
                                .listObjects {
                                    bucket = bucketName
                                    prefix = FILES_PREFIX
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
                                val checksumSha256 =
                                    run {
                                        // TODO: add caching

                                        val head =
                                            s3Client.headObject {
                                                bucket = bucketName
                                                key = it.key
                                                checksumMode = ChecksumMode.Enabled
                                            }

                                        head.checksumSha256!!.fromBase64ToHex()
                                    }

                                RepoIndex.File(
                                    it.key!!.toFilename(),
                                    it.size!!.toLong(),
                                    checksumSha256,
                                )
                            }
                        }.map { it.await() }
                }

            RepoIndex(files)
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
    ): T =
        s3Client.getObject(
            GetObjectRequest {
                bucket = bucketName
                key = filename.toKey()
                checksumMode = ChecksumMode.Enabled
            },
        ) { response ->
            block(
                ArchiveFileInfo(response.contentLength!!, response.checksumSha256!!.fromBase64ToHex()),
                response.body!!.toInputStream(),
            )
        }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        val digestInputStream = DigestInputStream(stream, digest)

        try {
            checkNotExists(filename)

            s3Client.putObject {
                bucket = bucketName
                key = filename.toKey()
                checksumSha256 = info.checksumSha256.fromHexToBase64()

                body = digestInputStream.asByteStream(contentLength = info.length)
            }
        } catch (e: S3Exception) {
            if (e.message == "Value for x-amz-checksum-sha256 header is invalid.") {
                throw ChecksumMismatch(info.checksumSha256, digest.digest().toHexString())
            } else {
                throw e
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

        try {
            val old = readMetadataFromS3() ?: RepositoryMetadata()
            val newMetadataBytes = Json.encodeToString(transform(old)).toByteArray()

            s3Client.putObject {
                bucket = bucketName
                key = ARCHIVE_METADATA_FILENAME
                checksumSha256 = newMetadataBytes.sha256().fromHexToBase64()

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
                        key = ARCHIVE_METADATA_FILENAME
                    },
                ) {
                    Json.decodeFromStream(it.body!!.toInputStream())
                }
        } catch (e: NoSuchKey) {
            null
        }

    companion object {
        private const val FILES_PREFIX = "files/"

        fun String.toKey() = "$FILES_PREFIX$this"

        fun String.toFilename() = this.removePrefix(FILES_PREFIX)

        suspend fun create(
            endpoint: URI,
            region: String,
            credentialsProvider: CredentialsProvider,
            bucketName: String,
            sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = S3Repository(
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
        }

        suspend fun open(
            endpoint: URI,
            region: String,
            credentialsProvider: CredentialsProvider,
            bucketName: String,
            sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = S3Repository(
            endpoint,
            region,
            credentialsProvider,
            bucketName,
            sharingDispatcher,
            ioDispatcher,
        ).apply {
            init()

            readMetadataFromS3() ?: throw S3LocationNotInitializedAsRepositoryException()
        }
    }
}
