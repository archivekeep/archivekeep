package org.archivekeep.app.android

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityUtil {
    private const val provider = "AndroidKeyStore"

    private val keyStore by lazy {
        KeyStore.getInstance(provider).apply {
            load(null)
        }
    }
    private val keyGenerator by lazy {
        KeyGenerator.getInstance(KEY_ALGORITHM_AES, provider)
    }

    fun encryptData(
        keyAlias: String,
        text: String,
    ): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = generateSecretKey(keyAlias)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedData = cipher.doFinal(text.toByteArray(charset("UTF-8")))
        val iv = cipher.iv
        return Pair(iv, encryptedData)
    }

    fun decryptData(
        keyAlias: String,
        iv: ByteArray,
        encryptedData: ByteArray,
    ): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = getSecretKey(keyAlias) ?: throw RuntimeException("Key not found")
        val gcmParameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
        return cipher.doFinal(encryptedData).toString(charset("UTF-8"))
    }

    private fun generateSecretKey(keyAlias: String): SecretKey =
        getSecretKey(keyAlias) ?: run {
            keyGenerator
                .apply {
                    init(
                        KeyGenParameterSpec
                            .Builder(keyAlias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                            .setBlockModes(BLOCK_MODE_GCM)
                            .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                            .build(),
                    )
                }.generateKey()
        }

    private fun getSecretKey(keyAlias: String) = (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
}
