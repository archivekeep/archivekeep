package org.archivekeep.files.exceptions

class DestinationExists(
    val path: String,
    message: String = "Path '$path' already stores existing file",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class ChecksumMismatch(
    val expected: String,
    val actual: String,
    message: String = "copied file has wrong checksum: got=$actual, expected=$expected",
) : RuntimeException(message)

class FileDoesntExist(
    val path: String,
    message: String = "Path '$path' is not file",
) : RuntimeException(message)
