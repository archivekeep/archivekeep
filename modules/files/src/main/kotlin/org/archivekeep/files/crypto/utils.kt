package org.archivekeep.files.crypto

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory
import com.nimbusds.jose.jwk.JWK
import kotlinx.serialization.json.Json

inline fun <reified T> parseVerifyDecodeJWS(
    string: String,
    signatureVerifier: JWSVerifier,
): T {
    val jwsObject = JWSObject.parse(string)

    if (!jwsObject.verify(signatureVerifier)) {
        throw RuntimeException("Verification failed")
    }

    return Json.decodeFromString<T>(jwsObject.payload.toString())
}

fun signAsJWS(
    string: String,
    signJWK: JWK,
): String {
    val signedJWS =
        JWSObject(
            JWSHeader.Builder(JWSAlgorithm.ES256).keyID(signJWK.keyID).build(),
            Payload(string),
        )

    signedJWS.sign(DefaultJWSSignerFactory().createJWSSigner(signJWK))

    return signedJWS.serialize()
}
