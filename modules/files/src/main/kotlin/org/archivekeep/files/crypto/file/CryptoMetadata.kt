package org.archivekeep.files.crypto.file

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEDecrypter
import com.nimbusds.jose.JWEEncrypter
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory
import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CryptoMetadata(
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

        val signedJWT =
            JWSObject(
                JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signJWK.keyID).build(),
                Payload(Json.encodeToString(JWSPayload(plain, jweObject.serialize()))),
            )

        signedJWT.sign(DefaultJWSSignerFactory().createJWSSigner(signJWK))

        return signedJWT.serialize()
    }

    companion object {
        fun verifyAndDecrypt(
            signatureVerifier: JWSVerifier,
            decrypter: JWEDecrypter,
            raw: String,
        ): CryptoMetadata {
            val jwsObject = JWSObject.parse(raw)

            if (!jwsObject.verify(signatureVerifier)) {
                throw RuntimeException("Verification failed")
            }

            val jwsPayload = Json.decodeFromString<JWSPayload>(jwsObject.payload.toString())

            val jweObject = JWEObject.parse(jwsPayload.encrypted)
            jweObject.decrypt(decrypter)

            return CryptoMetadata(
                jwsPayload.plain,
                Json.decodeFromString(jweObject.payload.toString()),
            )
        }
    }
}
