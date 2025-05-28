package org.archivekeep.files.driver.s3

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.net.URI

class S3Repository(
    val endpoint: URI,
    val region: String,
    val credentialsProvider: AwsCredentialsProvider,
    val bucketName: String,
) : Repo {
    val s3Client =
        S3Client
            .builder()
            .endpointOverride(endpoint)
            .region(Region.of("us-east-1"))
            .credentialsProvider(credentialsProvider)
            .forcePathStyle(true)
            .build()

    override val indexFlow: StateFlow<Loadable<RepoIndex>>
        get() = TODO("Not yet implemented")

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>>
        get() = TODO("Not yet implemented")

    override suspend fun index(): RepoIndex {
        TODO("Not yet implemented")
    }

    override suspend fun move(
        from: String,
        to: String,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun open(filename: String): Pair<ArchiveFileInfo, InputStream> {
        TODO("Not yet implemented")
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
    ) {
        val putRequest =
            PutObjectRequest
                .builder()
                .bucket(bucketName)
                .key("files/$filename")
                .build()

        s3Client.putObject(putRequest, RequestBody.fromInputStream(stream, info.length))
    }

    override suspend fun delete(filename: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getMetadata(): RepositoryMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        TODO("Not yet implemented")
    }
}
