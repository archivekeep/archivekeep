package org.archivekeep.utils.hashing

import java.security.MessageDigest

fun String.sha256(): String = hashString(this.toByteArray(), "SHA-256")

fun ByteArray.sha256(): String = hashString(this, "SHA-256")

private fun hashString(
    input: ByteArray,
    algorithm: String,
): String =
    MessageDigest
        .getInstance(algorithm)
        .digest(input)
        .fold("") { str, it -> str + "%02x".format(it) }
