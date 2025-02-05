package org.archivekeep.app.core.utils.identifiers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(
    with = StorageURI.Serializer::class,
)
data class StorageURI(
    val driver: String,
    val data: String,
) {
    companion object {
        val uriPrefix = "archivekeep:storage:"
    }

    override fun toString(): String = "$uriPrefix$driver:$data"

    fun substituteLabel(): String = "$driver:$data"

    class Serializer : KSerializer<StorageURI> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("StorageURI", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): StorageURI {
            val (a, b) =
                decoder
                    .decodeString()
                    .removePrefix(uriPrefix)
                    .split(":", limit = 2)

            return StorageURI(a, b)
        }

        override fun serialize(
            encoder: Encoder,
            value: StorageURI,
        ) {
            encoder.encodeString(value.toString())
        }
    }
}
