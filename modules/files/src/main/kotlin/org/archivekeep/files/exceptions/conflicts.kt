package org.archivekeep.files.exceptions

class DestinationExists(
    val path: String,
    message: String = "Path '$path' already stores existing file",
) : RuntimeException(message)

class FileDoesntExist(
    val path: String,
    message: String = "Path '$path' is not file",
) : RuntimeException(message)
