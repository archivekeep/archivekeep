package org.archivekeep.files.api.repository

const val ENCRYPTED_FILES_DIRECTORY = "encrypted-files"

const val ENCRYPTED_FILES_PATH_PREFIX = "$ENCRYPTED_FILES_DIRECTORY/"

const val ARCHIVE_METADATA_FILENAME = "metadata.json"

const val VAULT_FILENAME = "vault.jwe"

fun String.toEncryptedFilePath() = "$ENCRYPTED_FILES_PATH_PREFIX$this.enc"

fun String.fromEncryptedFilePath() = this.removePrefix(ENCRYPTED_FILES_PATH_PREFIX).removeSuffix(".enc")
