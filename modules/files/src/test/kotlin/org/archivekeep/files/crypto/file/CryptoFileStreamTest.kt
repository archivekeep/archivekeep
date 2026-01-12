package org.archivekeep.files.crypto.file

import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import kotlinx.coroutines.test.runTest
import org.archivekeep.utils.hashing.sha256
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertEquals

private val testData = "Content of file to be encrypted"
private val encryptedFileMetadataPlain =
    EncryptedFileMetadata.Plain(
        size = testData.toByteArray().size.toLong(),
        checksumSha256 = testData.sha256(),
    )

class CryptoFileStreamTest {
    @Test
    fun testWriteCryptoStream() =
        runTest {
            val ecJWK =
                ECKeyGenerator(Curve.P_256)
                    .keyID(UUID.randomUUID().toString())
                    .generate()

            val ecPublicJWK = ecJWK.toPublicJWK()

            val encryptedOutputStream = ByteArrayOutputStream()

            writeCryptoStream(
                encryptedFileMetadataPlain,
                ecJWK,
                ECDHEncrypter(ecPublicJWK),
                ByteArrayInputStream(testData.toByteArray()),
                encryptedOutputStream,
            )

            // encryption generates nondeterministic data, verify by decryption
            val decryptedContentsBuffer = ByteArrayOutputStream()

            val metadata =
                readCryptoStream(
                    ByteArrayInputStream(encryptedOutputStream.toByteArray()),
                    ECDSAVerifier(ecPublicJWK),
                    ECDHDecrypter(ecJWK),
                ) { metadata, inputStream ->
                    inputStream.copyTo(decryptedContentsBuffer)

                    metadata
                }

            assertEquals(testData, decryptedContentsBuffer.toString())
            assertEquals(encryptedFileMetadataPlain, metadata)
        }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun testReadCryptoStream() =
        runTest {
            val ecJWK =
                ECKey.parse(
                    @Suppress("ktlint:standard:max-line-length")
                    """{"kty":"EC","d":"wdxTB52S988qYYtPoDREmfU5JKbB6UbFa_JZ_3vvW9g","crv":"P-256","kid":"9dd4e53c-7ef7-408b-b515-57b505d9eb05","x":"iBQKa7Cwj7xIewFc8lABQPZDeUrtVN6kHWstFXmSk1E","y":"Dh7qCa5DY11RiSsG_8uwxPuMBD5ps3hV1xk71m5rsCk"}""",
                )

            val ecPublicJWK = ecJWK.toPublicJWK()

            val encryptedContent =
                @Suppress("ktlint:standard:max-line-length")
                "QXJjaGl2ZUtlZXAgRW5jcnlwdGVkIEZpbGUAAgAABIhleUpyYVdRaU9pSTVaR1EwWlRVell5MDNaV1kzTFRRd09HSXRZalV4TlMwMU4ySTFNRFZrT1dWaU1EVWlMQ0poYkdjaU9pSkZVekkxTmlKOS5leUp3YkdGcGJpSTZleUp6YVhwbElqb3pNU3dpWTJobFkydHpkVzFUYUdFeU5UWWlPaUkxTURreVl6TTNPRGxtTm1Ka05EaGxaakU0TURSaU1tRXpZekF5Wm1ZMU5HSTJOekUxTWprME5UZzFORGhoTnpZelpUWmpNR1k1TURJM01ESmpaakF3SW4wc0ltVnVZM0o1Y0hSbFpDSTZJbVY1U214alIzTnBUMjV6YVdFelVqVkphbTlwVWxWTmFVeERTbXBqYmxscFQybEtVVXhVU1RGT2FVbHpTVzVuYVU5cFNrcGlNMFo0WXpCME1HTnVVazVYUlZKMFZWWkZNRmRIUmxwUFYzaDRUakphVEUxSVJtcFpibFpxVmpKR2JHSllVWFJQVldNeVpVZEdUa2xwZDJsbFUwazJTV3c1YmxKVlNuTlJWbXgxVWpKek5GUlZiRTVOYWtacVUwWmFUMlZZVFROWlZWWlVVa1pHYmxaclZrbFJWbEphWVZoQmRFNXRaM3BqTWsxcFpsTjNhVnBYTldwSmFtOXBVVlJKTVU1clpFUlVVMGx6U1cxR2MxcDVTVFpKYTFaRVVrVm5kRkpXVFhKUlZFa3hUbXQwV0VsdU1DNVFRbGN0VDFWRGRXeExWbFpLTFdnd1YxSjFkVGt0WVZCWmNtNXhTakZKVDE5SlNEbE5WR0pWVlRZdFoyWldaelZSZUZSeWEyY3VPRUl4VkMxVFZ6SndOREkyZDJ3MFJDNW5la1Z4WkdoRmJIRnBiR2RoTVdocVJHdGZNRUpTZFU1cVgwMHpOVTh5TFhsVVNEa3djM1Y0WVhaMFJXUXdMVkpXT1dOUmMxWmhkMGxKTFZoTlUzZHBkMkpuTWpaYVMxcDZVbUZxYUU5MlRGSmxibmhTVmpZeUxVOWFaSHBYWm00MGIxcDJWSFZEUXkxalRraHhRVkU1U1dWSFN6Z3pjMEpaU1ROa1ZtbDJObXRNTFZKcWIyRTRkalphVVZsaFMzWXRaa3BzVjNGaloyMTVNV05HYUhWT1UwSnFSSGQwYWtwbU5UQTVWbkUzVmxoR1pVWllSbTlTTjI1WWQwNVlPRGxTY0UxQ1pWWlBhRWx6Ym5NemRYaEVaa1ZYTTBwb1ZqWjJlRVl3ZG1jdU4zUm5VRms0Ym1KQlZsTlpRblJOUm5kT01GQnFaeUo5LmVGS3MzTEg0MFRqdllOMjJ5V1VId1pQWGlYVzJPTnZUVUh6ZjlMa3FRZllBaDJpS3ZqSWZtWmQ5UTVlMHVYV2VaQzNpU3p0NkJUTU5HRVJuOE5JTWdRAGlzMTp8t+idGjkjRoT5CE4kN3jYQV8iW1l8U4GNLBtRe03zRSWImssOrzv3IF2Vs/CbBVYiMKhjN/7Av6cfbG3NqwFxmu50xLt+F3YRY77ghup+RPb8zZ1a8j9AICeVDbM9Hdt1ekiimUcllImfeqbuSq4tRdEGkcBIkLxdY17muTA="

            val decryptedContentsBuffer = ByteArrayOutputStream()

            val metadata =
                readCryptoStream(
                    ByteArrayInputStream(Base64.decode(encryptedContent)),
                    ECDSAVerifier(ecPublicJWK),
                    ECDHDecrypter(ecJWK),
                ) { metadata, inputStream ->
                    inputStream.copyTo(decryptedContentsBuffer)

                    metadata
                }

            assertEquals(testData, decryptedContentsBuffer.toString())
            assertEquals(encryptedFileMetadataPlain, metadata)
        }
}
