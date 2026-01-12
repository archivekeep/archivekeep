package org.archivekeep.files.api.exceptions

class InvalidFilename(
    val path: String,
    message: String = "File '$path' has invalid filename",
) : RuntimeException(message)
