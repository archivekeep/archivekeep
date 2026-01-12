package org.archivekeep.files.crypto.file

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEDecrypter
import com.nimbusds.jose.JWEEncrypter
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.Payload
import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.archivekeep.files.crypto.parseVerifyDecodeJWS
import org.archivekeep.files.crypto.signAsJWS

data class EncryptedFileMetadata(
    val plain: Plain,
    val private: Private,
) {
    @Serializable
    data class Plain(
        val size: Long,
        val checksumSha256: String,
    )

    @Serializable
    data class Private(
        val cipher: String,
        val secretKey: ByteArray,
        val iv: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Private

            if (cipher != other.cipher) return false
            if (!secretKey.contentEquals(other.secretKey)) return false
            if (!iv.contentEquals(other.iv)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = cipher.hashCode()
            result = 31 * result + secretKey.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }

    @Serializable
    private data class JWSPayload(
        val plain: Plain,
        val encrypted: String,
    )

    fun encryptAndSign(
        signJWK: JWK,
        encrypter: JWEEncrypter,
    ): String {
        val jweObject =
            JWEObject(
                JWEHeader.Builder(JWEAlgorithm.ECDH_ES_A256KW, EncryptionMethod.A256GCM).build(),
                Payload(Json.encodeToString(private)),
            )

        jweObject.encrypt(encrypter)

        return signAsJWS(Json.encodeToString(JWSPayload(plain, jweObject.serialize())), signJWK)
    }

    companion object {
        fun verifyAndDecrypt(
            signatureVerifier: JWSVerifier,
            decrypter: JWEDecrypter,
            raw: String,
        ): EncryptedFileMetadata {
            val jwsPayload = parseVerifyDecodeJWS<JWSPayload>(raw, signatureVerifier)

            val jweObject = JWEObject.parse(jwsPayload.encrypted)
            jweObject.decrypt(decrypter)

            return EncryptedFileMetadata(
                jwsPayload.plain,
                Json.decodeFromString(jweObject.payload.toString()),
            )
        }
    }
}
