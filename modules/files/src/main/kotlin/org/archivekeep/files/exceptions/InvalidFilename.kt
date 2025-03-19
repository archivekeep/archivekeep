package org.archivekeep.files.exceptions

class InvalidFilename(
    val path: String,
    message: String = "File '$path' has invalid filename",
) : RuntimeException(message)
