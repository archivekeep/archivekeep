package org.archivekeep.app.core.utils.identifiers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemRepositoryURIData
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCRepositoryURIData
import org.archivekeep.app.core.persistence.drivers.s3.S3RepositoryURIData
import org.archivekeep.app.core.persistence.platform.demo.DemoRepositoryURIData

@Serializable(
    with = RepositoryURI.Serializer::class,
)
data class RepositoryURI(
    val driver: String,
    val data: String,
) {
    val typedRepoURIData: TypedRepoURIData =
        when (driver) {
            FileSystemRepositoryURIData.ID -> FileSystemRepositoryURIData.fromSerialized(data)
            GRPCRepositoryURIData.ID -> GRPCRepositoryURIData.fromSerialized(data)
            S3RepositoryURIData.ID -> S3RepositoryURIData.fromSerialized(data)
            DemoRepositoryURIData.ID -> DemoRepositoryURIData.fromSerialized(data)
            else -> throw RuntimeException("Not supported $driver")
        }

    companion object {
        fun fromFull(uriText: String): RepositoryURI {
            val (driver, data) = uriText.split(":", limit = 2)

            return RepositoryURI(driver, data)
        }
    }

    override fun toString(): String = "$driver:$data"

    class Serializer : KSerializer<RepositoryURI> {
        val prefix = "archivekeep:repo:"

        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("RepositoryURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): RepositoryURI {
            val (a, b) =
                decoder
                    .decodeString()
                    .removePrefix(prefix)
                    .split(":", limit = 2)

            return RepositoryURI(a, b)
        }

        override fun serialize(
            encoder: Encoder,
            value: RepositoryURI,
        ) {
            encoder.encodeString("${prefix}$value")
        }
    }
}
