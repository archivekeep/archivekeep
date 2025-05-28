package org.archivekeep.files.driver.s3

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
import org.archivekeep.files.exceptions.ChecksumMismatch
import org.archivekeep.files.exceptions.DestinationExists
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.fromBase64ToHex
import org.archivekeep.utils.fromHexToBase64
import org.archivekeep.utils.loading.AutoRefreshLoadableFlow
import org.archivekeep.utils.loading.Loadable
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ChecksumMode
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.InputStream
import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Date

private const val METADATA_JSON_KEY = "metadata.json"

class S3Repository(
    endpoint: URI,
    region: String,
    credentialsProvider: AwsCredentialsProvider,
    val bucketName: String,
    val sharingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Repo {
    private val s3Client =
        S3Client
            .builder()
            .endpointOverride(endpoint)
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .forcePathStyle(true)
            .build()

    private val scope = CoroutineScope(sharingDispatcher + SupervisorJob())

    private val contentsLastChangeFlow = MutableStateFlow(Date())
    private val metadataLastChangeFlow = MutableStateFlow(Date())

    private val indexResource =
        AutoRefreshLoadableFlow(
            scope,
            ioDispatcher,
            updateTriggerFlow = contentsLastChangeFlow,
        ) {
            val files =
                coroutineScope {
                    s3Client
                        .listObjects { request ->
                            request.bucket(bucketName)
                        }.contents()
                        .map {
                            async {
                                val checksumSha256 =
                                    run {
                                        // TODO: add caching

                                        val head =
                                            s3Client.headObject { request ->
                                                request.bucket(bucketName)
                                                request.key(it.key())
                                            }

                                        head.checksumSHA256().fromBase64ToHex()
                                    }

                                RepoIndex.File(
                                    it.key().toFilename(),
                                    it.size().toLong(),
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
            readMetadataFromS3()
        }

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> = metadataResource.stateFlow

    override suspend fun index(): RepoIndex = indexResource.getFreshAndUpdateState()

    override suspend fun move(
        from: String,
        to: String,
    ) {
        try {
            checkNotExists(to)

            s3Client.copyObject { request ->
                request.sourceBucket(bucketName)
                request.sourceKey(from.toKey())
                request.destinationBucket(bucketName)
                request.destinationKey(to.toKey())
            }

            s3Client.deleteObject { request ->
                request.bucket(bucketName)
                request.key(from.toKey())
            }
        } finally {
            contentsLastChangeFlow.update { Date() }
        }
    }

    override suspend fun open(filename: String): Pair<ArchiveFileInfo, InputStream> {
        val responseInputStream =
            s3Client.getObject { request ->
                request.bucket(bucketName)
                request.key(filename.toKey())
                request.checksumMode(ChecksumMode.ENABLED)
            }

        val response = responseInputStream.response()

        return Pair(
            ArchiveFileInfo(response.contentLength(), response.checksumSHA256().fromBase64ToHex()),
            responseInputStream,
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

            s3Client.putObject(
                { request ->
                    request.bucket(bucketName)
                    request.key(filename.toKey())
                    request.checksumSHA256(info.checksumSha256.fromHexToBase64())
                },
                RequestBody.fromInputStream(digestInputStream, info.length),
            )
        } catch (e: S3Exception) {
            if (e.awsErrorDetails().errorMessage() == "Value for x-amz-checksum-sha256 header is invalid.") {
                throw ChecksumMismatch(info.checksumSha256, digest.digest().toHexString())
            }
        } finally {
            contentsLastChangeFlow.update { Date() }
        }
    }

    private fun checkNotExists(filename: String) {
        try {
            s3Client.headObject { request ->
                request.bucket(bucketName)
                request.key(filename.toKey())
            }

            throw DestinationExists(filename)
        } catch (e: NoSuchKeyException) {
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
            val old = readMetadataFromS3()
            val newMetadataString = Json.encodeToString(transform(old))

            s3Client.putObject(
                { request ->
                    request.bucket(bucketName)
                    request.key(METADATA_JSON_KEY)
                },
                RequestBody.fromString(newMetadataString),
            )
        } finally {
            metadataLastChangeFlow.update { Date() }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readMetadataFromS3() =
        try {
            s3Client
                .getObject { request ->
                    request.bucket(bucketName)
                    request.key(METADATA_JSON_KEY)
                }.use {
                    Json.decodeFromStream(it)
                }
        } catch (e: NoSuchKeyException) {
            RepositoryMetadata()
        }

    companion object {
        fun String.toKey() = "files/$this"

        fun String.toFilename() = this.removePrefix("files/")
    }
}
