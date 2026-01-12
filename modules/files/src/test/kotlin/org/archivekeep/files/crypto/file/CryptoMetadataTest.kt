package org.archivekeep.files.crypto.file

import com.nimbusds.jose.crypto.ECDHDecrypter
import com.nimbusds.jose.crypto.ECDHEncrypter
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

private val TEST_METADATA =
    EncryptedFileMetadata(
        plain =
            EncryptedFileMetadata.Plain(
                12353,
                "unimportant",
            ),
        private =
            EncryptedFileMetadata.Private(
                cipher = "SOME CIPHER",
                secretKey = "A SECRET KEY".toByteArray(),
                iv = "init vector".toByteArray(),
            ),
    )

class CryptoMetadataTest {
    @Test
    fun testEncryptAndSign() {
        val ecJWK =
            ECKeyGenerator(Curve.P_256)
                .keyID(UUID.randomUUID().toString())
                .generate()

        val ecPublicJWK = ecJWK.toPublicJWK()

        val encryptedContent = TEST_METADATA.encryptAndSign(ecJWK, ECDHEncrypter(ecPublicJWK))

        // encryption generates nondeterministic data, verify by decryption
        val decryptedMetadata = EncryptedFileMetadata.verifyAndDecrypt(ECDSAVerifier(ecPublicJWK), ECDHDecrypter(ecJWK), encryptedContent)
        assertEquals(TEST_METADATA, decryptedMetadata)
    }

    @Test
    fun testVerifyAndDecrypt() {
        val ecJWK =
            ECKey.parse(
                @Suppress("ktlint:standard:max-line-length")
                """{"kty":"EC","d":"i8nGEwWm3sFIW-WrVAdhNgeUYp388yXNJyim90P2SzU","crv":"P-256","kid":"20eab2fa-f81b-4273-924b-d630bbb0a025","x":"NLvQqqZnFrm5dfptE9sPIzfVfSOIJUcRHNJK6JoODu0","y":"jc_Rcnx2K4d9jgKG_CSNqxHyKbX9ipc9pHuNsPH2nhA"}""",
            )

        val ecPublicJWK = ecJWK.toPublicJWK()

        val encryptedContent =
            @Suppress("ktlint:standard:max-line-length")
            "eyJraWQiOiIyMGVhYjJmYS1mODFiLTQyNzMtOTI0Yi1kNjMwYmJiMGEwMjUiLCJhbGciOiJFUzI1NiJ9.eyJwbGFpbiI6eyJzaXplIjoxMjM1MywiY2hlY2tzdW1TaGEyNTYiOiJ1bmltcG9ydGFudCJ9LCJlbmNyeXB0ZWQiOiJleUpsY0dzaU9uc2lhM1I1SWpvaVJVTWlMQ0pqY25ZaU9pSlFMVEkxTmlJc0luZ2lPaUkxTW1sWlltOXlaVnB6VjE5bWQyZHVWalUyZGxoTVRsRndTbmRNZGt4U05FNU5jRFpPWXpoNVpHMVZJaXdpZVNJNklsWkxNMlkwTlZVMExWQjVjSEpRWWxGVFlsQkVibWRuT0V4WVFYbDZUSE56TkV4T2NrMWlTVVEyVEhjaWZTd2laVzVqSWpvaVFUSTFOa2REVFNJc0ltRnNaeUk2SWtWRFJFZ3RSVk1yUVRJMU5rdFhJbjAuaXNVaXJzZjhqWnJMbEFlejRkRmgtZVNiWVBJVHFFbFRhZUk4emZKWmRta0E1MmdOdzl4eUN3LnRJb1JiMWFOQkVoWDIyZnIuU1pfMzFlTWRVYzNfVGgzNFJ6TE1WRkNNM0dTREM0enEwWTJuRTVHLVg1b1gwTlM3NkpRaGl5ZVJZbnBNenk1TFJRQzhmN3ZwUXBoQnlKQWNkZFM3TWsyMkt0QlN5alNfVmxncWJJbXVGVnI3cWxRMFowSEhMdE5xTW9nc19HU2xqd0hBc1FTd3lTNGdSX0RiRC1YVm9CaUZ6MC1RdEVEcGdIRkYualJSM1NTR0FEZlp5cW1FUFoyR0hIdyJ9.-6hTeogouFflJUVJjIs4a6hLXf5Hrg7JNm0PtD93GCw_X9ZRL32M37ANzDHxwVTVjdTvuFwPmqL6PGoA9CDxLw"

        val decryptedMetadata = EncryptedFileMetadata.verifyAndDecrypt(ECDSAVerifier(ecPublicJWK), ECDHDecrypter(ecJWK), encryptedContent)

        assertEquals(TEST_METADATA, decryptedMetadata)
    }
}
