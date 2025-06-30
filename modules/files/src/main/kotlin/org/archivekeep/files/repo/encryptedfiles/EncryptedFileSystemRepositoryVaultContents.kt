package org.archivekeep.files.repo.encryptedfiles

import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class EncryptedFileSystemRepositoryVaultContents(
    val jwk: List<
        @Serializable(with = JWKSerializer::class)
        JWK,
        >,
    val currentFileSigningKeyID: String?,
    val currentFileEncryptionKeyID: String?,
) {
    val currentFileSigningKey: JWK?
        get() = jwk.firstOrNull { it.keyID == currentFileSigningKeyID }

    val currentFileEncryptionKey: JWK?
        get() = jwk.firstOrNull { it.keyID == currentFileEncryptionKeyID }
}

object JWKSerializer : KSerializer<JWK> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JWK")

    override fun serialize(
        encoder: Encoder,
        value: JWK,
    ) {
        val jsonElement = Json.parseToJsonElement(value.toJSONString())
        encoder.encodeSerializableValue(JsonElement.serializer(), jsonElement)
    }

    override fun deserialize(decoder: Decoder): JWK {
        val jsonElement = decoder.decodeSerializableValue(JsonElement.serializer())
        return JWK.parse(jsonElement.toString())
    }
}
