package org.archivekeep.files.repo.remote.grpc

import archivekeep.ArchiveServiceGrpcKt
import archivekeep.PersonalAccessTokenServiceGrpcKt.PersonalAccessTokenServiceCoroutineStub
import archivekeep.createPersonalAccessTokenRequest
import archivekeep.getArchiveRequest
import io.grpc.CallCredentials
import io.grpc.CompositeChannelCredentials
import io.grpc.Grpc.newChannelBuilderForAddress
import io.grpc.InsecureChannelCredentials
import io.grpc.LoadBalancerRegistry
import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.TlsChannelCredentials
import io.grpc.internal.PickFirstLoadBalancerProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.archivekeep.files.repo.Repo
import java.util.Date
import java.util.concurrent.Executor
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

val grpcPrefix = "grpc://"

data class Options(
    val credentials: BasicAuthCredentials?,
    val insecure: Boolean = false,
)

suspend fun openGrpcArchive(
    otherArchiveLocation: String,
    options: Options,
): Repo {
    val (hostPort, archiveName) = otherArchiveLocation.removePrefix(grpcPrefix).split("/", limit = 2)

    val (host, port) =
        if (hostPort.contains(":")) {
            val parts = hostPort.split(":", limit = 2)

            Pair(parts[0], parts[1].toInt())
        } else {
            Pair(hostPort, 24202)
        }

    return connect(host, port, archiveName, options)
}

@OptIn(ExperimentalEncodingApi::class)
suspend fun createPAT(
    address: String,
    port: Int,
    insecure: Boolean,
    credentials: BasicAuthCredentials,
): BasicAuthCredentials {
    val channelCredentials =
        if (insecure) InsecureChannelCredentials.create() else TlsChannelCredentials.create()

    val userCredentials =
        object : CallCredentials() {
            val META_DATA_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

            val raw = "${credentials.username}:${credentials.password}"
            val encoded = Base64.encode(raw.toByteArray())

            val full = "Basic $encoded"

            override fun applyRequestMetadata(
                requestInfo: RequestInfo,
                appExecutor: Executor,
                applier: MetadataApplier,
            ) {
                applier.apply(
                    Metadata().apply {
                        put(META_DATA_KEY, full)
                    },
                )
            }
        }

    val finalCredentials =
        CompositeChannelCredentials.create(
            channelCredentials,
            userCredentials,
        )

    return withContext(Dispatchers.IO) {
        val channel =
            newChannelBuilderForAddress(
                address,
                port,
                finalCredentials,
            ).build()

        try {
            val pat =
                PersonalAccessTokenServiceCoroutineStub(channel).createPersonalAccessToken(
                    createPersonalAccessTokenRequest {
                        this.name = "ArchiveKeep Local ${Date()}"
                    },
                )

            BasicAuthCredentials(
                username = credentials.username,
                password = pat.token,
            )
        } finally {
            channel.shutdown()
        }
    }
}

@OptIn(ExperimentalEncodingApi::class)
suspend fun connect(
    address: String,
    port: Int,
    archiveName: String,
    options: Options,
): RemoteGrpcRepository {
    LoadBalancerRegistry.getDefaultRegistry().register(PickFirstLoadBalancerProvider())

    val channelCredentials =
        if (options.insecure) InsecureChannelCredentials.create() else TlsChannelCredentials.create()
    val userCredentials =
        options.credentials?.let {
            object : CallCredentials() {
                val META_DATA_KEY = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

                val raw = "${options.credentials.username}:${options.credentials.password}"
                val encoded = Base64.encode(raw.toByteArray())

                val full = "Basic $encoded"

                override fun applyRequestMetadata(
                    requestInfo: RequestInfo,
                    appExecutor: Executor,
                    applier: MetadataApplier,
                ) {
                    applier.apply(
                        Metadata().apply {
                            put(META_DATA_KEY, full)
                        },
                    )
                }
            }
        }

    val finalCredentials =
        if (userCredentials != null) {
            CompositeChannelCredentials.create(
                channelCredentials,
                userCredentials,
            )
        } else {
            channelCredentials
        }

    val channel =
        newChannelBuilderForAddress(
            address,
            port,
            finalCredentials,
        ).build()

    try {
        ArchiveServiceGrpcKt.ArchiveServiceCoroutineStub(channel).getArchive(
            getArchiveRequest {
                this.name = archiveName
            },
        )
    } catch (e: Exception) {
        channel.shutdown()
        throw RuntimeException(e)
    }

    return RemoteGrpcRepository(channel, archiveName)
}

fun Exception.isNotAuthorized(): Boolean =
    (this is StatusException && status.description?.endsWith("not authorized") == true) || ((this.cause as? Exception)?.isNotAuthorized() ?: false)
