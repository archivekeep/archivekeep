package org.archivekeep.core.exceptions

class DestinationExists (
    val path: String,
    message: String = "Path '${path}' already stores existing file"
): RuntimeException(message)
