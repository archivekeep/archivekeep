package org.archivekeep.files.repo.remote.grpc

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.archivekeep.files.exceptions.UnsupportedFeatureException
import org.archivekeep.files.internal.grpc.Api
import org.archivekeep.files.internal.grpc.ArchiveServiceGrpcKt
import org.archivekeep.files.internal.grpc.archiveFile
import org.archivekeep.files.internal.grpc.bytesChunk
import org.archivekeep.files.internal.grpc.chunkOrNull
import org.archivekeep.files.internal.grpc.headOrNull
import org.archivekeep.files.internal.grpc.listArchiveFilesRequest
import org.archivekeep.files.internal.grpc.uploadArchiveFileRequest
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.ObservableRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import java.io.Closeable
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

val chunkSize = 64 * 1024

class RemoteGrpcRepository(
    private val channel: ManagedChannel,
    private val archiveName: String,
    // TODO: subscription to remote
    private val modifChannel: MutableSharedFlow<Date> = MutableStateFlow(Date()),
) : Repo,
    Closeable,
    ObservableRepo {
    private val remoteService = ArchiveServiceGrpcKt.ArchiveServiceCoroutineStub(channel)

    override fun close() {
        channel.shutdown().awaitTermination(30, TimeUnit.SECONDS)
    }

    override suspend fun index(): RepoIndex {
        val result =
            remoteService.listArchiveFiles(
                listArchiveFilesRequest {
                    parent = archiveName
                },
            )

        return RepoIndex(
            files =
                result.filesList.map {
                    RepoIndex.File(
                        path = it.name.removePrefix(archiveName).removePrefix("/files/"),
                        checksumSha256 = it.digestsMap["SHA256"] ?: throw RuntimeException("No SHA256"),
                    )
                },
        )
    }

    override suspend fun move(
        from: String,
        to: String,
    ) {
        reportOnCompletion {
            remoteService.moveArchiveFile(
                request =
                    Api.MoveArchiveFileRequest
                        .newBuilder()
                        .setName(fileReferenceName(from))
                        .setDestinationName(fileReferenceName(to))
                        .build(),
            )
        }
    }

    override suspend fun open(filename: String): Pair<ArchiveFileInfo, InputStream> {
        val supervisor = SupervisorJob()
        val supervisorScope = CoroutineScope(Dispatchers.IO + coroutineContext + supervisor)

        try {
            val a =
                remoteService
                    .downloadArchiveFile(
                        request =
                            Api.DownloadArchiveFileRequest
                                .newBuilder()
                                .setName(fileReferenceName(filename))
                                .build(),
                    ).flowOn(Dispatchers.IO)

            val ch = a.produceIn(supervisorScope)

            val b =
                try {
                    ch.receive()
                } catch (e: Exception) {
                    throw RuntimeException("Collect: $e", e)
                }

            val fi =
                b.headOrNull?.let { head ->
                    ArchiveFileInfo(
                        length = head.length,
                        checksumSha256 =
                            head.digestsMap["SHA256"]
                                ?: throw RuntimeException("no SHA256"),
                    )
                } ?: throw RuntimeException("not head")

            val pipedInputStream = PipedInputStream()

            val out =
                withContext(Dispatchers.IO) {
                    PipedOutputStream(pipedInputStream)
                }

            CoroutineScope(Dispatchers.IO + coroutineContext).launch {
                try {
                    ch.consumeEach { m ->
                        val chunk = m.chunkOrNull ?: throw RuntimeException("expected chunk")

                        chunk.chunk.writeTo(out)
                        out.flush()
                    }
                    println("consume finished: $filename")
                } catch (e: Exception) {
                    println("Error: $e")
                    throw e
                } finally {
                    out.close()
                    supervisor.cancel()
                    ch.cancel()
                    println("ended: $filename")
                }
            }

            return Pair(
                fi,
                pipedInputStream,
            )
        } catch (e: Exception) {
            supervisor.cancel()

            throw RuntimeException("Open remote file: $e", e)
        }
    }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
    ) {
        reportOnCompletion {
            remoteService.uploadArchiveFile(
                flow {
                    emit(
                        uploadArchiveFileRequest {
                            head =
                                archiveFile {
                                    name = fileReferenceName(filename)
                                    length = info.length

                                    digests.put("SHA256", info.checksumSha256)
                                }
                        },
                    )

                    stream.buffered().use { input ->
                        val buff = ByteArray(size = chunkSize)

                        while (true) {
                            val sz = input.read(buff)
                            if (sz <= 0) break

                            emit(
                                uploadArchiveFileRequest {
                                    chunk =
                                        bytesChunk {
                                            chunk = ByteString.copyFrom(buff, 0, sz)
                                        }
                                },
                            )
                        }
                    }
                },
            )
        }
    }

    override suspend fun delete(filename: String) {
        reportOnCompletion {
            TODO("Not yet implemented")
        }
    }

    override suspend fun getMetadata(): RepositoryMetadata = throw UnsupportedFeatureException("GRPC repository Metadata")

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata): Unit =
        throw UnsupportedFeatureException("Update GRPC repository metadata")

    fun fileReferenceName(filename: String): String = "$archiveName/files/$filename"

    override val observable: ObservableRepo = this

    override val indexFlow: Flow<Loadable<RepoIndex>> =
        modifChannel
            .mapToLoadable { index() }
            .shareIn(GlobalScope, SharingStarted.WhileSubscribed())

    override val metadataFlow: Flow<Loadable<RepositoryMetadata>> =
        flowOf(
            Loadable.Failed(UnsupportedFeatureException()),
        )

    private inline fun <R> reportOnCompletion(call: () -> R): R {
        try {
            return call()
        } finally {
            GlobalScope.launch {
                modifChannel.emit(Date())
            }
        }
    }
}
