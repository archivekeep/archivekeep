package org.archivekeep.files.crypto.file

import com.nimbusds.jose.JWEDecrypter
import com.nimbusds.jose.JWEEncrypter
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.jwk.JWK
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

const val HEADER = "ArchiveKeep Encrypted File"

fun writeCryptoStream(
    fileMetadata: CryptoMetadata.Plain,
    signJWK: JWK,
    encrypter: JWEEncrypter,
    inputStream: InputStream,
    outputStream: OutputStream,
) {
    outputStream.write(HEADER.toByteArray())
    outputStream.write(0)

    outputStream.write(2)

    val cryptoMetadata =
        CryptoMetadata(
            plain = fileMetadata,
            private =
                CryptoMetadata.Private(
                    "AES-128-CFB",
                    KeyGenerator
                        .getInstance("AES")
                        .let {
                            it.init(128)
                            it.generateKey()!!.encoded
                        },
                    iv = Random.nextBytes(16),
                ),
        )
    val rawMetadata = cryptoMetadata.encryptAndSign(signJWK, encrypter).toByteArray()

    outputStream.write(
        ByteBuffer
            .allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .apply { putInt(rawMetadata.size + 100) }
            .array(),
    )
    outputStream.write(rawMetadata)
    outputStream.write(0)
    outputStream.write(Random.nextBytes(99))

    val cipher = Cipher.getInstance("AES/CFB/NoPadding")
    val ivParameterSpec = IvParameterSpec(cryptoMetadata.private.iv)
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(cryptoMetadata.private.secretKey, "AES"), ivParameterSpec)

    val cipherOutputStream = CipherOutputStream(outputStream, cipher)
    inputStream.copyTo(cipherOutputStream)
    cipherOutputStream.close()
}

suspend fun <T> readCryptoStream(
    inputStream: InputStream,
    signatureVerifier: JWSVerifier,
    decrypter: JWEDecrypter,
    handler: suspend (plainMetadata: CryptoMetadata.Plain, decryptedStream: InputStream) -> T,
): T {
    val buffered = inputStream.buffered()

    val header = buffered.readString(HEADER.length + 5)
    if (header != HEADER) {
        throw RuntimeException("Not crypto file, the actual header is $header instead of $HEADER")
    }

    val fileFormat = buffered.read()
    if (fileFormat != 2) {
        throw RuntimeException("Unsupported crypto format: $fileFormat")
    }

    val metadataSize = buffered.readIntBE()
    if (metadataSize > 10_000) {
        throw RuntimeException("Metadata size $metadataSize is too big")
    }
    val metadataRawBytes = ByteArray(metadataSize)

    val bytesRead = buffered.read(metadataRawBytes)
    if (bytesRead != metadataSize) {
        throw RuntimeException("Read $bytesRead for metadata instead of full size $metadataSize")
    }

    val metadata = CryptoMetadata.verifyAndDecrypt(signatureVerifier, decrypter, String(metadataRawBytes, 0, metadataRawBytes.indexOf(0)))

    return decryptCryptoStreamContents(buffered, metadata, handler)
}

suspend fun <T> decryptCryptoStreamContents(
    input: InputStream,
    metadata: CryptoMetadata,
    handler: suspend (plainMetadata: CryptoMetadata.Plain, decryptedStream: InputStream) -> T,
): T {
    if (metadata.private.cipher != "AES-128-CFB") {
        throw RuntimeException("Unsupported cipher ${metadata.private.cipher}")
    }

    val cipher = Cipher.getInstance("AES/CFB/NoPadding")
    val ivParameterSpec = IvParameterSpec(metadata.private.iv)
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(metadata.private.secretKey, "AES"), ivParameterSpec)

    val cipherInputStream = CipherInputStream(input, cipher)

    return handler(metadata.plain, cipherInputStream)
}

fun InputStream.readString(maxLength: Int? = null): String {
    val buffer = ByteArrayOutputStream()

    while (true) {
        val byte = this.read()

        if (byte == -1 || byte == 0) {
            break
        }

        buffer.write(byte)

        if (maxLength != null && buffer.size() == maxLength) {
            break
        }
    }

    return buffer.toString(Charsets.UTF_8.name())
}

fun InputStream.readIntBE(): Int {
    val bytes = ByteArray(4)
    read(bytes)
    return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).int
}
